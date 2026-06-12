package com.nyora.hasan72341.core.network.webview

import android.content.Context
import android.util.AndroidRuntimeException
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.MainThread
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import com.nyora.hasan72341.core.exceptions.CloudFlareException
import com.nyora.hasan72341.core.network.CommonHeaders
import com.nyora.hasan72341.core.network.cookies.MutableCookieJar
import com.nyora.hasan72341.core.network.proxy.ProxyProvider
import com.nyora.hasan72341.core.parser.MangaRepository
import com.nyora.hasan72341.core.parser.ParserMangaRepository
import com.nyora.hasan72341.core.util.ext.configureForParser
import com.nyora.hasan72341.core.util.ext.printStackTraceDebug
import com.nyora.hasan72341.core.util.ext.sanitizeHeaderValue
import com.nyora.hasan72341.mihon.parsers.model.MangaSource
import com.nyora.hasan72341.mihon.parsers.util.nullIfEmpty
import com.nyora.hasan72341.mihon.parsers.util.runCatchingCancellable
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.ranges.contains

@Singleton
class WebViewExecutor @Inject constructor(
	@ApplicationContext private val context: Context,
	private val proxyProvider: ProxyProvider,
	private val cookieJar: MutableCookieJar,
	private val mangaRepositoryFactoryProvider: Provider<MangaRepository.Factory>,
) {

	private var webViewCached: WeakReference<WebView>? = null
	private val mutex = Mutex()

	val defaultUserAgent: String? by lazy {
		try {
			WebSettings.getDefaultUserAgent(context).stripWebViewMarkers()
		} catch (e: AndroidRuntimeException) {
			e.printStackTraceDebug("WebViewExecutor::defaultUserAgent")
			// Probably WebView is not available
			null
		}
	}

    suspend fun evaluateJs(
        baseUrl: String?,
        script: String,
        timeoutMs: Long = 15000L,
        preserveCookies: Boolean = false
    ): String? = mutex.withLock {
        withContext(Dispatchers.Main.immediate) {
            val webView = obtainWebView()
            val handler = android.os.Handler(android.os.Looper.getMainLooper())

            try {
                if (baseUrl.isNullOrEmpty()) {
                    return@withContext suspendCoroutine { cont ->
                        webView.evaluateJavascript(script) { cont.resume(it.takeUnless { r -> r == "null" }) }
                    }
                }

                val baseUri = android.net.Uri.parse(baseUrl)
                val originalHost = baseUri.host

                suspendCoroutine { continuation ->
                    var hasResumed = false

                    val resumeOnce: (String?) -> Unit = { result ->
                        if (!hasResumed) {
                            hasResumed = true
                            handler.removeCallbacksAndMessages(null)
                            // Immediately stop further loading/polling
                            webView.stopLoading()
                            continuation.resume(result)
                        }
                    }

                    val contentPoller = object : Runnable {
                        val startTime = System.currentTimeMillis()
                        override fun run() {
                            if (hasResumed) return
                            if (System.currentTimeMillis() - startTime >= timeoutMs) {
                                return
                            }
                            webView.evaluateJavascript(script) { result ->
                                if (hasResumed) return@evaluateJavascript
                                val content = result?.takeUnless { it == "null" }
                                if (!content.isNullOrBlank()) {
                                    println("DEBUG: Content found via polling. Returning immediately.")
                                    resumeOnce(content)
                                } else {
                                    handler.postDelayed(this, 1000)
                                }
                            }
                        }
                    }

                    webView.webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val url = request?.url ?: return false
                            val requestHost = url.host
                            if (originalHost != null && requestHost != null && requestHost.contains(originalHost)) {
                                return false
                            }
                            println("DEBUG: Blocked redirect to external domain: $url")
                            return true
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            if (hasResumed || url == "about:blank") return
                            println("DEBUG: onPageFinished. Checking content...")
                            view?.evaluateJavascript(script) { result ->
                                if (hasResumed) return@evaluateJavascript
                                val content = result?.takeUnless { it == "null" }
                                if (!content.isNullOrBlank()) {
                                    println("DEBUG: Content found on pageFinished. Returning immediately.")
                                    resumeOnce(content)
                                }
                            }
                        }
                    }

                    val headers = mapOf("Accept-Language" to "en-EN,en;q=0.9")
                    if (preserveCookies) {
                        webView.loadDataWithBaseURL(baseUrl, " ", "text/html", null, null)
                    } else {
                        webView.loadUrl(baseUrl, headers)
                    }

                    handler.postDelayed(contentPoller, 1000)

                    handler.postDelayed({
                        if (!hasResumed) {
                            println("ERROR: Overall operation timed out.")
                            resumeOnce(null)
                        }
                    }, timeoutMs)
                }
            } finally {
                // If already resumed, stopLoading() was called; this is a safety call.
                webView.stopLoading()
            }
        }
    }

    suspend fun tryResolveCaptcha(exception: CloudFlareException, timeout: Long): Boolean = mutex.withLock {
		runCatchingCancellable {
			withContext(Dispatchers.Main.immediate) {
				val webView = obtainWebView()
				try {
					exception.source.getUserAgent()?.let {
						webView.settings.userAgentString = it
					}
					withTimeout(timeout) {
						suspendCancellableCoroutine { cont ->
							webView.webViewClient = CaptchaContinuationClient(
								cookieJar = cookieJar,
								targetUrl = exception.url,
								continuation = cont,
							)
							webView.loadUrl(exception.url)
						}
					}
				} finally {
					webView.reset()
				}
			}
		}.onFailure { e ->
			exception.addSuppressed(e)
			e.printStackTraceDebug("WebViewExecutor::tryResolveCaptcha")
		}.isSuccess
	}

    @MainThread
    private fun obtainWebView(): WebView = webViewCached?.get() ?: WebView(context).also {
        // Use a clean (non-"wv") browser UA so the Cloudflare clearance cookie this WebView
        // earns is valid for the OkHttp requests that reuse it — and so sites that block the
        // Android WebView marker (Manganato et al.) behave the same as on web/mac/iOS.
        it.configureForParser(defaultUserAgent ?: it.settings.userAgentString?.stripWebViewMarkers())
        webViewCached = WeakReference(it)
    }

    /**
     * Strips the Android WebView fingerprint (`; wv` and `Version/4.0`) from a system WebView
     * user-agent, leaving a clean Chrome-mobile UA indistinguishable from a real browser.
     */
    private fun String.stripWebViewMarkers(): String =
        replace(Regex(";?\\s*wv"), "")
            .replace(Regex("Version/\\d+\\.\\d+\\s+"), "")
            .replace(Regex("\\s{2,}"), " ")
            .trim()

	private fun MangaSource.getUserAgent(): String? {
		val repository = mangaRepositoryFactoryProvider.get().create(this) as? ParserMangaRepository
		return repository?.getRequestHeaders()?.get(CommonHeaders.USER_AGENT)
	}

    /**
     * Passes a Cloudflare challenge for [url] in a real WebView (Chromium runs the JS/managed
     * challenge), writing the `cf_clearance` cookie into the shared cookie jar so subsequent
     * OkHttp requests to that host go through. Returns true if clearance was obtained.
     */
    suspend fun resolveCloudflare(url: String, timeoutMs: Long = 25_000L): Boolean {
        // Load the actual blocked URL so Cloudflare presents the managed challenge for that path
        // (browse pages may be unprotected while an API endpoint is gated). cf_clearance is
        // HttpOnly so it can't be polled via document.cookie; instead wait until the page is no
        // longer the interstitial. The clearance cookie is written to the shared CookieManager
        // by then and reused by OkHttp on the retry.
        val script = """
            (function () {
              var t = (document.title || '').toLowerCase();
              if (t.indexOf('just a moment') >= 0 || t.indexOf('attention required') >= 0 || t.indexOf('please wait') >= 0) return null;
              if (document.getElementById('challenge-running') || document.getElementById('cf-challenge-running') || document.querySelector('#challenge-form, #cf-please-wait')) return null;
              return 'ok';
            })()
        """.trimIndent()
        return runCatching {
            evaluateJs(
                baseUrl = url,
                script = script,
                timeoutMs = timeoutMs,
                preserveCookies = true,
            )
        }.getOrNull() != null
    }

    @MainThread
    fun getDefaultUserAgentSync() = runCatching {
        obtainWebView().settings.userAgentString.stripWebViewMarkers().sanitizeHeaderValue().trim().nullIfEmpty()
    }.onFailure { e ->
        e.printStackTraceDebug()
    }.getOrNull()

	@MainThread
	private fun WebView.reset() {
		stopLoading()
		webViewClient = WebViewClient()
		settings.userAgentString = defaultUserAgent
		loadDataWithBaseURL(null, " ", "text/html", null, null)
		clearHistory()
	}
}
