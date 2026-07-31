package com.nyora.hasan72341.core.network.imageproxy

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * Decrypts MangaPlus page images. Their bytes are XOR-encrypted with a per-page key; the data-driven
 * `mangaplus` engine attaches that key as the request header [KEY_HEADER] (via the page's headers).
 * This interceptor strips the header (so the CDN never sees it), downloads the encrypted bytes, and
 * XOR-decrypts them — the app-side equivalent of the native MangaPlusParser's image interceptor.
 * It is a no-op for every other request (no key header → passthrough).
 */
class MangaPlusImageInterceptor : Interceptor {

	override fun intercept(chain: Interceptor.Chain): Response {
		val original = chain.request()
		val key = original.header(KEY_HEADER)
		if (key.isNullOrEmpty()) {
			return chain.proceed(original)
		}
		val response = chain.proceed(original.newBuilder().removeHeader(KEY_HEADER).build())
		val body = response.body ?: return response
		val contentType = response.header("Content-Type") ?: "image/jpeg"
		val decrypted = body.bytes().decodeXorCipher(key)
		return response.newBuilder()
			.body(decrypted.toResponseBody(contentType.toMediaTypeOrNull()))
			.build()
	}

	// Mirrors MangaPlusParser.decodeXorCipher: key is hex pairs -> byte stream; plaintext[i] =
	// cipher[i] xor keyStream[i % keyStream.size].
	private fun ByteArray.decodeXorCipher(key: String): ByteArray {
		val keyStream = key.chunked(2).mapNotNull { it.toIntOrNull(16) }
		if (keyStream.isEmpty()) return this
		return ByteArray(size) { i -> (this[i].toInt() xor keyStream[i % keyStream.size]).toByte() }
	}

	companion object {
		const val KEY_HEADER = "X-Nyora-Mangaplus-Key"
	}
}
