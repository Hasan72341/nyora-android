package com.nyora.hasan72341.ai.data

import com.nyora.hasan72341.core.network.BaseHttpClient
import com.nyora.hasan72341.core.prefs.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.nyora.hasan72341.tachiyomi.network.await
import com.nyora.hasan72341.mihon.parsers.util.runCatchingCancellable
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRepository @Inject constructor(
	private val settings: AppSettings,
	@BaseHttpClient private val okHttp: OkHttpClient,
) {
	private val json = Json { ignoreUnknownKeys = true }

	suspend fun getCompletion(messages: List<ChatMessage>, useJsonMode: Boolean = false): Result<String> = withContext(Dispatchers.IO) {
		runCatchingCancellable {
			val requestBody = ChatRequest(
				model = settings.aiModel,
				messages = messages,
				response_format = if (useJsonMode) ResponseFormat("json_object") else null
			)

			val endpoint = settings.aiEndpoint.removeSuffix("/")
			val request = Request.Builder()
				.url("$endpoint/chat/completions")
				.header("Authorization", "Bearer ${settings.aiApiKey}")
				.post(json.encodeToString(requestBody).toRequestBody("application/json".toMediaType()))
				.build()

			val response = okHttp.newCall(request).await()
			if (!response.isSuccessful) {
				throw IOException("Unexpected response code: ${response.code} ${response.message}")
			}
			val body = response.body?.string() ?: throw IOException("Empty response")
			val chatResponse = json.decodeFromString<ChatResponse>(body)
			chatResponse.choices.firstOrNull()?.message?.content ?: throw IOException("No completion found")
		}
	}
}
