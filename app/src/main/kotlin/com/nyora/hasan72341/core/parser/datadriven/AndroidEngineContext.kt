package com.nyora.hasan72341.core.parser.datadriven

import app.nyora.data.engine.AntiBotKind
import app.nyora.data.engine.DomNode
import app.nyora.data.engine.EngineContext
import app.nyora.data.engine.HtmlDocument
import app.nyora.data.engine.HttpRequest
import app.nyora.data.engine.HttpResponse
import app.nyora.data.engine.SourcePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.util.concurrent.ConcurrentHashMap

/**
 * The [EngineContext] a data-driven [app.nyora.data.engine.SourceEngine] runs against, backed by the
 * app's shared manga OkHttp client. Routing through that client is what gives every data-driven
 * source the app's Cloudflare interceptor, cookie jar and per-source header handling for free — the
 * same treatment the bundled parsers get — so no separate anti-bot solver is wired here.
 *
 * Ported from the library's DefaultEngineContext (JVM harness) with the throwaway OkHttp client
 * swapped for the injected [MangaHttpClient] one. [prefs] is per-source; a fresh context is built
 * per repository instance so the key/value store is naturally namespaced by source.
 */
class AndroidEngineContext(
    private val client: OkHttpClient,
    private val userAgent: String = DEFAULT_UA,
) : EngineContext {

    override val prefs: SourcePrefs = InMemoryPrefs()

    override suspend fun http(request: HttpRequest): HttpResponse = withContext(Dispatchers.IO) {
        val builder = Request.Builder().url(request.url)

        val headers = LinkedHashMap<String, String>()
        headers["User-Agent"] = userAgent
        headers["Accept"] = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
        headers["Accept-Language"] = "en-US,en;q=0.9"
        headers.putAll(request.headers)
        builder.headers(headers.toHeaders())

        when (request.method.uppercase()) {
            "POST" -> {
                val body = when {
                    request.form != null -> FormBody.Builder().apply {
                        request.form!!.forEach { (k, v) -> add(k, v) }
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
                body = resp.body?.string().orEmpty(),
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

    companion object {
        const val DEFAULT_UA =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
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
