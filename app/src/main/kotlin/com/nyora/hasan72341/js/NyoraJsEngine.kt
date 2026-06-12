package com.nyora.hasan72341.js

import android.annotation.SuppressLint
import android.content.Context
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.nyora.hasan72341.core.exceptions.CloudFlareException
import com.nyora.hasan72341.core.network.MangaHttpClient
import com.nyora.hasan72341.core.network.webview.WebViewExecutor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs the shared Nyora JS parser bundle (`assets/parsers.bundle.js`, the same one used
 * by the desktop/iOS apps) inside a single hidden Android WebView.
 *
 * This mirrors the iOS [JSParserEngine] rather than the desktop GraalVM host: the WebView
 * (Chromium) gives us a real DOM for `context.parseHTML` (via `DOMParser`), native
 * Promises/async, and the modern JS globals the parsers assume — so no hand-built Jsoup
 * DOM bridge and no polyfills are needed. The parser `context.httpGet/httpPost` calls are
 * routed to OkHttp (the `@MangaHttpClient`, which carries the Cloudflare interceptor +
 * cookie jar) via an async request/reply bridge.
 */
@Singleton
class NyoraJsEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    @MangaHttpClient private val httpClient: OkHttpClient,
    private val webViewExecutor: WebViewExecutor,
    private val otaUpdater: NyoraJsOtaUpdater,
) {
    private val mainScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val pending = ConcurrentHashMap<String, CompletableDeferred<String>>()
    private val seq = AtomicInteger(0)

    @Volatile private var webView: WebView? = null
    @Volatile private var ready: CompletableDeferred<Unit>? = null

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun ensureLoaded() {
        ready?.let { it.await(); return }
        withContext(Dispatchers.Main.immediate) {
            if (ready == null) {
                val gate = CompletableDeferred<Unit>()
                ready = gate
                val wv = WebView(context)
                wv.settings.javaScriptEnabled = true
                wv.settings.domStorageEnabled = true
                wv.addJavascriptInterface(Bridge(), "NyoraAndroid")
                webView = wv
                val bundle = withContext(Dispatchers.IO) {
                    otaUpdater.bundle()
                        ?: context.assets.open("parsers.bundle.js").bufferedReader().use { it.readText() }
                }
                val html = buildString {
                    append("<!DOCTYPE html><html><head><meta charset=\"utf-8\"></head><body>")
                    append("<script>").append(BRIDGE_JS).append("</script>")
                    append("<script>").append(bundle).append("</script>")
                    append("<script>try{NyoraAndroid.onReady()}catch(e){NyoraAndroid.log('ready-fail '+e)}</script>")
                    append("</body></html>")
                }
                wv.loadDataWithBaseURL("https://nyora.local/", html, "text/html", "utf-8", null)
            }
        }
        ready?.await()
    }

    /**
     * Invokes one parser method (`list` | `details` | `pages`) for [jsSourceId] and returns
     * the parser's result as a JSON string. [argsJson] is the JSON args object.
     */
    suspend fun run(jsSourceId: String, method: String, argsJson: String): String {
        ensureLoaded()
        val token = "c" + seq.incrementAndGet()
        val deferred = CompletableDeferred<String>()
        pending[token] = deferred
        val b64 = Base64.encodeToString(argsJson.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        withContext(Dispatchers.Main.immediate) {
            webView?.evaluateJavascript(
                "void window.__runParser(${token.jsq()},${jsSourceId.jsq()},${method.jsq()},'$b64')",
                null,
            )
        }
        return try {
            withTimeout(60_000) { deferred.await() }
        } finally {
            pending.remove(token)
        }
    }

    private fun resolveHttp(id: String, ok: Boolean, payload: String) {
        val b64 = Base64.encodeToString(payload.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        mainScope.launch {
            webView?.evaluateJavascript("window.__resolveHttp(${id.jsq()},$ok,'$b64')", null)
        }
    }

    private inner class Bridge {
        @JavascriptInterface
        fun onReady() {
            ready?.let { if (!it.isCompleted) it.complete(Unit) }
        }

        @JavascriptInterface
        fun log(message: String) {
            Log.d("NyoraJS", message)
        }

        /** Fire-and-forget: runs the fetch off-thread, then resolves the JS promise by id. */
        @JavascriptInterface
        fun requestHttp(id: String, url: String, method: String, body: String?, headersJson: String?, domain: String) {
            ioScope.launch {
                try {
                    if (method == "HEAD_URL") {
                        resolveHttp(id, true, getFinalUrl(url, domain))
                    } else {
                        resolveHttp(id, true, httpFetchWithCloudflare(method, url, domain, body, headersJson))
                    }
                } catch (e: Throwable) {
                    resolveHttp(id, false, e.message ?: "network error")
                }
            }
        }

        @JavascriptInterface
        fun postResult(token: String, ok: Boolean, payload: String) {
            val d = pending.remove(token) ?: return
            if (ok) d.complete(payload) else d.completeExceptionally(RuntimeException(payload))
        }
    }

    private fun getFinalUrl(url: String, domain: String): String {
        val builder = Request.Builder()
            .url(url)
            .head()
        if (domain.isNotBlank()) {
            builder.header("Referer", "https://$domain/")
        }
        httpClient.newCall(builder.build()).execute().use { response ->
            return response.request.url.toString()
        }
    }

    /**
     * Runs [httpFetch]; if the request is challenged by Cloudflare, solves the challenge in a
     * real Android WebView (Chromium passes the JS/managed challenge and writes the
     * `cf_clearance` cookie into the shared cookie jar), then retries the request once.
     */
    private suspend fun httpFetchWithCloudflare(
        method: String,
        url: String,
        domain: String,
        body: String?,
        headersJson: String?,
    ): String {
        return try {
            httpFetch(method, url, domain, body, headersJson)
        } catch (e: CloudFlareException) {
            solveCloudflare(e.url.ifBlank { url })
            httpFetch(method, url, domain, body, headersJson)
        }
    }

    /** Passes the Cloudflare challenge for [url] in a WebView, populating the shared cookie jar. */
    private suspend fun solveCloudflare(url: String) {
        runCatching { webViewExecutor.resolveCloudflare(url) }
            .onFailure { Log.w("NyoraJS", "Cloudflare solve failed for $url", it) }
    }

    private fun httpFetch(method: String, url: String, domain: String, body: String?, headersJson: String?): String {
        val origin = runCatching {
            val u = java.net.URI(url)
            if (u.scheme != null && u.host != null) "${u.scheme}://${u.host}" else null
        }.getOrNull() ?: if (domain.isNotBlank()) "https://$domain" else ""
        val isPost = method.equals("POST", ignoreCase = true)
        val builder = Request.Builder()
            .url(url)
            .method(method, if (isPost) (body ?: "").toRequestBody() else null)
        // Mirror the mac/iOS DirectFetch request shape so sources behave identically across
        // platforms. The (cleaned, non-"wv") User-Agent is added by CommonHeadersInterceptor.
        builder.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        builder.header("Accept-Language", "en-US,en;q=0.9")
        val referer = if (domain.isNotBlank()) "https://$domain/" else if (origin.isNotEmpty()) "$origin/" else null
        if (referer != null) builder.header("Referer", referer)
        if (isPost) {
            builder.header("Content-Type", "application/x-www-form-urlencoded")
            if (origin.isNotEmpty()) builder.header("Origin", origin)
            builder.header("X-Requested-With", "XMLHttpRequest")
        }
        if (!headersJson.isNullOrBlank() && headersJson != "{}") {
            runCatching {
                (Json.parseToJsonElement(headersJson) as? JsonObject)?.forEach { (k, v) ->
                    builder.header(k, (v as? JsonPrimitive)?.content ?: v.toString())
                }
            }
        }
        httpClient.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) throw RuntimeException("HTTP ${response.code}")
            return response.body?.string().orEmpty()
        }
    }

    /** Quote a (simple/ASCII) string as a JS/JSON string literal. */
    private fun String.jsq(): String = JsonPrimitive(this).toString()

    private companion object {
        /**
         * Injected before the bundle. Defines `window.__context` (httpGet/httpPost via the
         * async native bridge, parseHTML via the browser DOMParser) and `window.__runParser`
         * (calls NyoraParsers.getParser + the requested method, posts the JSON result back).
         * Ported from the iOS bridgeScript.
         */
        private const val BRIDGE_JS = """
(function () {
  var pend = {}, n = 0;
  function dec(b64) { return decodeURIComponent(escape(atob(b64))); }
  window.__resolveHttp = function (id, ok, b64) {
    var p = pend[id]; if (!p) return; delete pend[id];
    var t = dec(b64);
    if (ok) p.resolve(t); else p.reject(new Error(t));
  };
  function mergeHeaders(parser, extra) {
    var out = {};
    if (parser && parser.headers) { for (var k in parser.headers) out[k] = parser.headers[k]; }
    if (extra) { for (var k2 in extra) out[k2] = extra[k2]; }
    return Object.keys(out).length ? JSON.stringify(out) : null;
  }
  window.__context = {
    httpGet: function (url, extraHeaders, parser) {
      return new Promise(function (res, rej) {
        var id = 'h' + (++n); pend[id] = { resolve: res, reject: rej };
        var actualHeaders = extraHeaders;
        var actualParser = parser;
        if (arguments.length < 3 && extraHeaders && typeof extraHeaders === 'object' && !Array.isArray(extraHeaders)) {
          actualParser = extraHeaders;
          actualHeaders = null;
        }
        NyoraAndroid.requestHttp(
          id,
          url,
          'GET',
          null,
          mergeHeaders(actualParser, actualHeaders),
          (actualParser && actualParser.domain) ? actualParser.domain : ''
        );
      });
    },
    httpPost: function (url, body, extraHeaders, parser) {
      return new Promise(function (res, rej) {
        var id = 'h' + (++n); pend[id] = { resolve: res, reject: rej };
        NyoraAndroid.requestHttp(id, url, 'POST', body || '', mergeHeaders(parser, extraHeaders), (parser && parser.domain) ? parser.domain : '');
      });
    },
    parseHTML: function (html) { return new DOMParser().parseFromString(html, 'text/html'); },
    decodeContent: function (s) { return s; }
  };
  window.__runParser = async function (token, sid, method, b64) {
    try {
      var args = JSON.parse(dec(b64));
      var p = NyoraParsers.getParser(sid, window.__context);
      if (!p) throw new Error('parser not found: ' + sid);

      // AUTO-REDIRECTION SUPPORT
      try {
        if (method === 'list' && args.page === 1 && !args.filter.query) {
          var testUrl = 'https://' + p.domain + '/';
          var finalUrl = await new Promise(function(res, rej) {
            var id = 'r' + (++n); pend[id] = { resolve: res, reject: rej };
            NyoraAndroid.requestHttp(id, testUrl, 'HEAD_URL', null, null, p.domain);
          });
          if (finalUrl && finalUrl.startsWith('http')) {
            var newDomain = new URL(finalUrl).hostname;
            if (newDomain && newDomain !== p.domain) {
              console.log('[Redir] Domain changed: ' + p.domain + ' -> ' + newDomain);
              p.domain = newDomain;
            }
          }
        }
      } catch (e) { console.error('[Redir] check failed', e); }

      var r;
      if (method === 'list') r = await p.getListPage(args.page, args.order, args.filter || {});
      else if (method === 'details') r = await p.getDetails({ id: args.url, url: args.url, title: args.title, source: { id: sid, name: sid } });
      else if (method === 'pages') r = await p.getPages({ id: args.url, url: args.url, branch: args.branch, source: { id: sid } });
      else throw new Error('unknown method ' + method);
      NyoraAndroid.postResult(token, true, JSON.stringify(r === undefined ? null : r));
    } catch (e) {
      NyoraAndroid.postResult(token, false, (e && e.message) ? e.message : String(e));
    }
  };
})();
"""
    }
}
