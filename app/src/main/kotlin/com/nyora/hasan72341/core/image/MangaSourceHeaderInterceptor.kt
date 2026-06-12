package com.nyora.hasan72341.core.image

import coil3.intercept.Interceptor
import coil3.network.httpHeaders
import coil3.request.ImageResult
import com.nyora.hasan72341.core.model.unwrap
import com.nyora.hasan72341.core.network.CommonHeaders
import com.nyora.hasan72341.core.util.ext.mangaSourceKey

class MangaSourceHeaderInterceptor : Interceptor {

	override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
		// Forward the source for ALL source types (not just native MangaParserSource) so that
		// CommonHeadersInterceptor can attach the right per-source headers (e.g. the image
		// Referer that Manganato & other JS sources require for cover thumbnails).
		val mangaSource = chain.request.extras[mangaSourceKey]?.unwrap() ?: return chain.proceed()
		val request = chain.request
		val newHeaders = request.httpHeaders.newBuilder()
			.set(CommonHeaders.MANGA_SOURCE, mangaSource.name)
			.build()
		val newRequest = request.newBuilder()
			.httpHeaders(newHeaders)
			.build()
		return chain.withRequest(newRequest).proceed()
	}
}
