package com.nyora.hasan72341.core.network

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Fixes the *Lib family (MangaLib / HentaiLib / SlashLib / …), which all talk to
 * `api.cdnlibs.org`. Two recent server-side changes broke every *Lib title:
 *
 *  1. The API now rejects requests without a browser Origin/Referer (404 with none,
 *     403 with only Site-Id). The parser already sends the per-site `Site-Id`, so we
 *     just add a valid *Lib Origin/Referer — cdnlibs does NOT require it to match the
 *     Site-Id (verified), so one constant domain covers the whole family.
 *
 *  2. A manga's `summary` used to be a plain string but is now a ProseMirror rich-text
 *     document (`{"type":"doc","content":[…]}`). kotatsu-parsers does
 *     `json.getString("summary")`, which throws and fails the whole details parse. We
 *     flatten that object back to plain text in the response so the parser is happy.
 *
 * Both are surgical and only touch api.cdnlibs.org; a parser's own headers always win.
 * Mirrors nyora-shared's LibApiHeadersInterceptor (helper/desktop) so all variants
 * behave identically.
 */
class LibApiHeadersInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!request.url.host.endsWith("cdnlibs.org", ignoreCase = true)) {
            return chain.proceed(request)
        }
        val builder = request.newBuilder()
        if (request.header("Origin") == null) builder.header("Origin", ORIGIN)
        if (request.header("Referer") == null) builder.header("Referer", "$ORIGIN/")
        val response = chain.proceed(builder.build())

        // Only the JSON API host carries `summary`; image hosts are left untouched.
        if (!request.url.host.equals("api.cdnlibs.org", ignoreCase = true)) return response
        val contentType = response.header("Content-Type").orEmpty()
        if (!contentType.contains("json", ignoreCase = true)) return response
        val body = response.body ?: return response

        val text = body.string()
        val fixed = try {
            flattenSummary(text)
        } catch (_: Throwable) {
            text
        }
        val media = body.contentType() ?: "application/json".toMediaTypeOrNull()
        return response.newBuilder().body(fixed.toResponseBody(media)).build()
    }

    /** If `data.summary` is a ProseMirror object, replace it with its plain text. */
    private fun flattenSummary(json: String): String {
        val root = JSONObject(json)
        val data = root.optJSONObject("data") ?: return json
        val summary = data.opt("summary")
        if (summary !is JSONObject) return json
        data.put("summary", proseMirrorToText(summary).trim())
        return root.toString()
    }

    private fun proseMirrorToText(node: JSONObject): String {
        val sb = StringBuilder()
        appendNode(node, sb)
        return sb.toString()
    }

    private fun appendNode(node: JSONObject, sb: StringBuilder) {
        when (node.optString("type")) {
            "text" -> sb.append(node.optString("text"))
            "hardBreak" -> sb.append('\n')
        }
        val content: JSONArray? = node.optJSONArray("content")
        if (content != null) {
            for (i in 0 until content.length()) {
                content.optJSONObject(i)?.let { appendNode(it, sb) }
            }
        }
        when (node.optString("type")) {
            "paragraph", "heading", "blockquote", "listItem" -> sb.append('\n')
        }
    }

    private companion object {
        const val ORIGIN = "https://mangalib.me"
    }
}
