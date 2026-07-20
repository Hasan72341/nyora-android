package com.nyora.hasan72341.scrobbling.bangumi.data

import okhttp3.Interceptor
import okhttp3.Response
import okio.IOException
import com.nyora.hasan72341.core.network.CommonHeaders
import com.nyora.hasan72341.scrobbling.common.data.ScrobblerStorage
import com.nyora.hasan72341.scrobbling.common.domain.ScrobblerAuthRequiredException
import com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblerService
import java.net.HttpURLConnection

private const val USER_AGENT_BANGUMI = "Nyora (https://github.com/Nyora-Manga)"

class BangumiInterceptor(private val storage: ScrobblerStorage) : Interceptor {

	override fun intercept(chain: Interceptor.Chain): Response {
		val sourceRequest = chain.request()
		val request = sourceRequest.newBuilder()
		request.header(CommonHeaders.USER_AGENT, USER_AGENT_BANGUMI)
		val isAuthRequest = sourceRequest.url.pathSegments.contains("oauth")
		if (!isAuthRequest) {
			storage.accessToken?.let {
				request.header(CommonHeaders.AUTHORIZATION, "Bearer $it")
			}
		}
		val response = chain.proceed(request.build())
		if (!isAuthRequest && response.code == HttpURLConnection.HTTP_UNAUTHORIZED) {
			throw ScrobblerAuthRequiredException(ScrobblerService.BANGUMI)
		}
		if (!response.isSuccessful && !response.isRedirect) {
			throw IOException("${response.code} ${response.message}")
		}
		return response
	}
}
