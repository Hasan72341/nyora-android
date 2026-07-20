package com.nyora.hasan72341.scrobbling.mangabaka.data

import okhttp3.Interceptor
import okhttp3.Response
import okio.IOException
import com.nyora.hasan72341.core.network.CommonHeaders
import com.nyora.hasan72341.scrobbling.common.data.ScrobblerStorage
import com.nyora.hasan72341.scrobbling.common.domain.ScrobblerAuthRequiredException
import com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblerService
import java.net.HttpURLConnection

class MangaBakaInterceptor(private val storage: ScrobblerStorage) : Interceptor {

	override fun intercept(chain: Interceptor.Chain): Response {
		val sourceRequest = chain.request()
		val request = sourceRequest.newBuilder()
		// Auth requests hit mangabaka.org/auth/oauth2/*; API requests hit
		// api.mangabaka.org and carry the bearer token.
		val isAuthRequest = sourceRequest.url.pathSegments.contains("oauth2")
		if (!isAuthRequest) {
			storage.accessToken?.let {
				request.header(CommonHeaders.AUTHORIZATION, "Bearer $it")
			}
		}
		val response = chain.proceed(request.build())
		if (!isAuthRequest && response.code == HttpURLConnection.HTTP_UNAUTHORIZED) {
			throw ScrobblerAuthRequiredException(ScrobblerService.MANGABAKA)
		}
		if (!response.isSuccessful && !response.isRedirect) {
			throw IOException("${response.code} ${response.message}")
		}
		return response
	}
}
