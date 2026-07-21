package com.nyora.hasan72341.core.parser.datadriven

import app.nyora.data.engine.AntiBotKind
import app.nyora.data.engine.DomNode
import app.nyora.data.engine.EngineContext
import app.nyora.data.engine.HtmlDocument
import app.nyora.data.engine.HttpRequest
import app.nyora.data.engine.HttpResponse
import app.nyora.data.engine.SourcePrefs
import com.nyora.hasan72341.mihon.parsers.model.MangaSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.util.concurrent.ConcurrentHashMap

/**
 * The [EngineContext] a data-driven [app.nyora.data.engine.SourceEngine] runs against, backed by the
 * app's shared manga OkHttp client so it gets the Cloudflare interceptor, cookie jar and headers.
 * [prefs] is per-source (a fresh context is built per repository).
 */
class AndroidEngineContext(
    private val client: OkHttpClient,
    private val source: MangaSource,
) : EngineContext {

    override val prefs: SourcePrefs = InMemoryPrefs()

    override suspend fun http(request: HttpRequest): HttpResponse = withContext(Dispatchers.IO) {
        val builder = Request.Builder().url(request.url)
            // Tag with the source so CommonHeadersInterceptor/CloudFlareInterceptor can key off it.
            .tag(MangaSource::class.java, source)

        // Transport hint (not a real HTTP header): engines set X-Nyora-Encoding=multipart to request
        // a multipart/form-data body with raw values (e.g. Natsu's advanced_search JSON fields).
        val isMultipart = request.headers.entries
            .firstOrNull { it.key.equals(HDR_ENCODING, ignoreCase = true) }
            ?.value?.equals("multipart", ignoreCase = true) == true

        // Don't set User-Agent here: cf_clearance is bound to the WebView UA that
        // CommonHeadersInterceptor supplies; a UA set here would mismatch it and re-trigger Cloudflare.
        val headers = LinkedHashMap<String, String>()
        headers["Accept"] = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
        headers["Accept-Language"] = "en-US,en;q=0.9"
        request.headers.forEach { (k, v) -> if (!k.equals(HDR_ENCODING, ignoreCase = true)) headers[k] = v }
        builder.headers(headers.toHeaders())

        when (request.method.uppercase()) {
            "POST" -> {
                val body = when {
                    // Multipart form with raw field values (values are NOT pre-encoded).
                    request.form != null && isMultipart -> MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .apply { request.form!!.forEach { (k, v) -> addFormDataPart(k, v) } }
                        .build()

                    // urlencoded form. Engine keys/values are already URL-encoded (Madara's
                    // madara_load_more template, `query.urlEncoded()`), so use addEncoded to avoid
                    // double-encoding — plain add() would turn `vars%5Bs%5D` into `vars%255Bs%255D`
                    // and the request would return nothing.
                    request.form != null -> FormBody.Builder().apply {
                        request.form!!.forEach { (k, v) -> addEncoded(k, v) }
                    }.build()

                    request.body != null -> {
                        val ct = request.headers.entries
                            .firstOrNull { it.key.equals("Content-Type", ignoreCase = true) }?.value
                            ?: "application/x-www-form-urlencoded"
                        request.body!!.toRequestBody(ct.toMediaType())
                    }

                    else -> FormBody.Builder().build()
                }
                builder.post(body)
            }

            "GET" -> builder.get()
            else -> builder.method(request.method.uppercase(), null)
        }

        client.newCall(builder.build()).execute().use { resp ->
            HttpResponse(
                url = resp.request.url.toString(),
                code = resp.code,
                // Bounded read so a hostile or runaway source page can't OOM the app; a normal
                // HTML/JSON listing is far smaller than this cap.
                body = resp.peekBody(MAX_RESPONSE_BYTES).string(),
                headers = resp.headers.toMultimap().mapValues { it.value.joinToString(", ") },
            )
        }
    }

    override fun parseHtml(html: String, baseUrl: String): HtmlDocument =
        JsoupDomNode(Jsoup.parse(html, baseUrl))

    override suspend fun solveAntiBot(kind: AntiBotKind, url: String): Map<String, String> {
        // The shared OkHttp client already carries the Cloudflare interceptor + cookie jar, so a
        // separate cookie-solving pass here would be redundant.
        return emptyMap()
    }

    private class InMemoryPrefs : SourcePrefs {
        private val map = ConcurrentHashMap<String, String>()
        override fun getString(key: String): String? = map[key]
        override fun putString(key: String, value: String?) {
            if (value == null) map.remove(key) else map[key] = value
        }
    }

    private companion object {
        // HTML/JSON listing pages are well under this; the cap only guards against abuse.
        private const val MAX_RESPONSE_BYTES = 16L * 1024 * 1024 // 16 MiB
        // Engine transport hint: value "multipart" -> send the form as multipart/form-data (raw values).
        private const val HDR_ENCODING = "X-Nyora-Encoding"
    }
}

/** Jsoup-backed node satisfying both the opaque [HtmlDocument] marker and the engine [DomNode] surface. */
private class JsoupDomNode(private val el: Element) : HtmlDocument, DomNode {
    override fun select(cssQuery: String): List<DomNode> = el.select(cssQuery).map { JsoupDomNode(it) }
    override fun selectFirst(cssQuery: String): DomNode? = el.selectFirst(cssQuery)?.let { JsoupDomNode(it) }
    override fun attr(name: String): String = el.attr(name)
    override fun text(): String = el.text()
    override fun data(): String = el.data()
    override fun baseUri(): String = el.baseUri()
    override fun tagName(): String = el.tagName()
    override fun parent(): DomNode? = el.parent()?.let { JsoupDomNode(it) }
    override fun lastElementSibling(): DomNode? =
        (el.parent()?.children())?.lastOrNull()?.let { JsoupDomNode(it) }
    override fun lastElementChild(): DomNode? = el.children().lastOrNull()?.let { JsoupDomNode(it) }
}
