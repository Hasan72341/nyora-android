package com.nyora.hasan72341.scrobbling.mangabaka.data

import android.content.Context
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.db.MangaDatabase
import com.nyora.hasan72341.core.util.ext.toRequestBody
import com.nyora.hasan72341.tachiyomi.network.await
import com.nyora.hasan72341.mihon.parsers.util.json.getStringOrNull
import com.nyora.hasan72341.mihon.parsers.util.json.mapJSON
import com.nyora.hasan72341.mihon.parsers.util.parseJson
import com.nyora.hasan72341.scrobbling.common.data.ScrobblerRepository
import com.nyora.hasan72341.scrobbling.common.data.ScrobblerStorage
import com.nyora.hasan72341.scrobbling.common.data.ScrobblingEntity
import com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblerManga
import com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblerMangaInfo
import com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblerService
import com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblerType
import com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblerUser
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

private const val REDIRECT_URI = "nyora://mangabaka-auth"
private const val OAUTH_URL = "https://mangabaka.org/auth/oauth2"
private const val API_URL = "https://api.mangabaka.org"
private const val WEB_URL = "https://mangabaka.org"
private const val SCOPE = "library.read+library.write+profile+offline_access+openid"

@Singleton
class MangaBakaRepository @Inject constructor(
	@ApplicationContext context: Context,
	@ScrobblerType(ScrobblerService.MANGABAKA) private val okHttp: OkHttpClient,
	@ScrobblerType(ScrobblerService.MANGABAKA) private val storage: ScrobblerStorage,
	private val db: MangaDatabase,
) : ScrobblerRepository {

	private val clientId = context.getString(R.string.mangabaka_clientId)
	private val codeVerifier: String by lazy(::generateCodeVerifier)

	override val oauthUrl: String
		get() = "$OAUTH_URL/authorize?response_type=code" +
			"&client_id=$clientId" +
			"&redirect_uri=$REDIRECT_URI" +
			"&scope=$SCOPE" +
			"&code_challenge=${generateCodeChallenge(codeVerifier)}" +
			"&code_challenge_method=S256"

	override val isAuthorized: Boolean
		get() = storage.accessToken != null

	override val cachedUser: ScrobblerUser?
		get() = storage.user

	override suspend fun authorize(code: String?) {
		val body = FormBody.Builder()
		body.add("client_id", clientId)
		if (code != null) {
			body.add("grant_type", "authorization_code")
			body.add("code", code)
			body.add("redirect_uri", REDIRECT_URI)
			body.add("code_verifier", codeVerifier)
		} else {
			body.add("grant_type", "refresh_token")
			body.add("refresh_token", checkNotNull(storage.refreshToken))
		}
		val request = Request.Builder()
			.post(body.build())
			.url("$OAUTH_URL/token")
		val response = okHttp.newCall(request.build()).await().parseJson()
		storage.accessToken = response.getString("access_token")
		storage.refreshToken = response.getStringOrNull("refresh_token") ?: storage.refreshToken
	}

	override suspend fun loadUser(): ScrobblerUser {
		val request = Request.Builder().get().url("$API_URL/v1/my/profile")
		val data = okHttp.newCall(request.build()).await().parseJson().dataObject()
		return MangaBakaUser(data).also { storage.user = it }
	}

	override suspend fun unregister(mangaId: String) {
		return db.getScrobblingDao().delete(ScrobblerService.MANGABAKA.id, mangaId)
	}

	override fun logout() {
		storage.clear()
	}

	override suspend fun findManga(query: String, offset: Int): List<ScrobblerManga> {
		val url = "$API_URL/v1/series/search".toHttpUrl().newBuilder()
			.addQueryParameter("q", query)
			.build()
		val request = Request.Builder().url(url).get().build()
		val response = okHttp.newCall(request).await().parseJson()
		return response.getJSONArray("data").mapJSON { ScrobblerManga(it, query) }
	}

	override suspend fun getMangaInfo(id: Long): ScrobblerMangaInfo {
		val request = Request.Builder().get().url("$API_URL/v1/series/$id")
		val data = okHttp.newCall(request.build()).await().parseJson().dataObject()
		return ScrobblerMangaInfo(data)
	}

	override suspend fun createRate(mangaId: String, scrobblerMangaId: Long) {
		putLibrary(scrobblerMangaId, JSONObject().put("state", STATE_READING).put("progress_chapter", 0))
		saveState(scrobblerMangaId, mangaId)
	}

	override suspend fun updateRate(rateId: Int, mangaId: String, chapter: Int) {
		val seriesId = rateId.toLong()
		putLibrary(seriesId, JSONObject().put("state", STATE_READING).put("progress_chapter", chapter))
		saveState(seriesId, mangaId)
	}

	override suspend fun updateRate(rateId: Int, mangaId: String, rating: Float, status: String?, comment: String?) {
		val seriesId = rateId.toLong()
		val payload = JSONObject()
			.put("state", status ?: STATE_READING)
			.put("rating", rating.toInt().coerceIn(0, 100))
		putLibrary(seriesId, payload)
		saveState(seriesId, mangaId)
	}

	// MangaBaka upserts the library entry and answers 201 { status, data:true }
	// with no useful body, so we re-read the entry afterwards.
	private suspend fun putLibrary(seriesId: Long, payload: JSONObject) {
		val request = Request.Builder()
			.url("$API_URL/v1/my/library/$seriesId")
			.post(payload.toRequestBody())
			.build()
		okHttp.newCall(request).await().close()
	}

	private suspend fun saveState(seriesId: Long, mangaId: String) {
		val request = Request.Builder().get().url("$API_URL/v1/my/library/$seriesId")
		val data = okHttp.newCall(request.build()).await().parseJson().dataObject()
		val entity = ScrobblingEntity(
			scrobbler = ScrobblerService.MANGABAKA.id,
			id = seriesId.toInt(),
			mangaId = mangaId,
			targetId = seriesId,
			status = data.getStringOrNull("state") ?: STATE_READING,
			chapter = data.optDouble("progress_chapter", 0.0).toInt(),
			comment = "",
			rating = (data.optDouble("rating", 0.0).toFloat() / 100f).coerceIn(0f, 1f),
		)
		db.getScrobblingDao().upsert(entity)
	}

	private fun JSONObject.dataObject(): JSONObject = optJSONObject("data") ?: this

	private fun ScrobblerManga(json: JSONObject, sourceTitle: String): ScrobblerManga {
		val id = json.getLong("id")
		val title = json.getStringOrNull("title") ?: json.getStringOrNull("native_title").orEmpty()
		return ScrobblerManga(
			id = id,
			name = title,
			altName = json.getStringOrNull("native_title"),
			cover = json.coverUrl(),
			url = "$WEB_URL/series/$id",
			isBestMatch = title.equals(sourceTitle, ignoreCase = true),
		)
	}

	private fun ScrobblerMangaInfo(json: JSONObject) = ScrobblerMangaInfo(
		id = json.getLong("id"),
		name = json.getStringOrNull("title").orEmpty(),
		cover = json.coverUrl().orEmpty(),
		url = "$WEB_URL/series/${json.getLong("id")}",
		descriptionHtml = json.getStringOrNull("description").orEmpty(),
	)

	@Suppress("FunctionName")
	private fun MangaBakaUser(json: JSONObject) = ScrobblerUser(
		id = json.optLong("id", 0L),
		nickname = json.getStringOrNull("username") ?: json.getStringOrNull("name").orEmpty(),
		avatar = json.getStringOrNull("avatar"),
		service = ScrobblerService.MANGABAKA,
	)

	// MangaBaka returns nested cover-size variants; return the first URL found.
	private fun JSONObject.coverUrl(): String? {
		val cover = optJSONObject("cover") ?: return null
		cover.getStringOrNull("default")?.let { return it }
		cover.optJSONObject("raw")?.getStringOrNull("url")?.let { return it }
		cover.optJSONObject("x250")?.getStringOrNull("x1")?.let { return it }
		return null
	}

	private fun generateCodeVerifier(): String {
		val bytes = ByteArray(50)
		SecureRandom().nextBytes(bytes)
		return Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE)
	}

	private fun generateCodeChallenge(verifier: String): String {
		val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray())
		return Base64.encodeToString(digest, Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE)
	}

	private companion object {
		const val STATE_READING = "reading"
	}
}
