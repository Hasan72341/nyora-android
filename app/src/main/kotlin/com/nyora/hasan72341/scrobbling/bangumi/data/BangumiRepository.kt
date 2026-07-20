package com.nyora.hasan72341.scrobbling.bangumi.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
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
import javax.inject.Inject
import javax.inject.Singleton

private const val REDIRECT_URI = "nyora://bangumi-auth"
private const val OAUTH_URL = "https://bgm.tv/oauth"
private const val API_URL = "https://api.bgm.tv"
private const val WEB_URL = "https://bgm.tv"
private const val MANGA_PAGE_SIZE = 20

@Singleton
class BangumiRepository @Inject constructor(
	@ApplicationContext context: Context,
	@ScrobblerType(ScrobblerService.BANGUMI) private val okHttp: OkHttpClient,
	@ScrobblerType(ScrobblerService.BANGUMI) private val storage: ScrobblerStorage,
	private val db: MangaDatabase,
) : ScrobblerRepository {

	private val clientId = context.getString(R.string.bangumi_clientId)
	private val clientSecret = context.getString(R.string.bangumi_clientSecret)

	override val oauthUrl: String
		get() = "$OAUTH_URL/authorize?client_id=$clientId" +
			"&response_type=code&redirect_uri=$REDIRECT_URI"

	override val isAuthorized: Boolean
		get() = storage.accessToken != null

	override val cachedUser: ScrobblerUser?
		get() = storage.user

	override suspend fun authorize(code: String?) {
		val body = FormBody.Builder()
		body.add("client_id", clientId)
		body.add("client_secret", clientSecret)
		if (code != null) {
			body.add("grant_type", "authorization_code")
			body.add("redirect_uri", REDIRECT_URI)
			body.add("code", code)
		} else {
			body.add("grant_type", "refresh_token")
			body.add("redirect_uri", REDIRECT_URI)
			body.add("refresh_token", checkNotNull(storage.refreshToken))
		}
		val request = Request.Builder()
			.post(body.build())
			.url("$OAUTH_URL/access_token")
		val response = okHttp.newCall(request.build()).await().parseJson()
		storage.accessToken = response.getString("access_token")
		storage.refreshToken = response.getString("refresh_token")
	}

	override suspend fun loadUser(): ScrobblerUser {
		val request = Request.Builder().get().url("$API_URL/v0/me")
		val response = okHttp.newCall(request.build()).await().parseJson()
		return BangumiUser(response).also { storage.user = it }
	}

	override suspend fun unregister(mangaId: String) {
		return db.getScrobblingDao().delete(ScrobblerService.BANGUMI.id, mangaId)
	}

	override fun logout() {
		storage.clear()
	}

	override suspend fun findManga(query: String, offset: Int): List<ScrobblerManga> {
		val url = "$API_URL/v0/search/subjects".toHttpUrl().newBuilder()
			.addQueryParameter("limit", MANGA_PAGE_SIZE.toString())
			.addQueryParameter("offset", offset.toString())
			.build()
		val payload = JSONObject()
			.put("keyword", query)
			.put("sort", "match")
			.put("filter", JSONObject().put("type", JSONArray().put(1))) // 1 = book/manga
		val request = Request.Builder().url(url).post(payload.toRequestBody()).build()
		val response = okHttp.newCall(request).await().parseJson()
		return response.getJSONArray("data").mapJSON { ScrobblerManga(it, query) }
	}

	override suspend fun getMangaInfo(id: Long): ScrobblerMangaInfo {
		val request = Request.Builder().get().url("$API_URL/v0/subjects/$id")
		val response = okHttp.newCall(request.build()).await().parseJson()
		return ScrobblerMangaInfo(response)
	}

	override suspend fun createRate(mangaId: String, scrobblerMangaId: Long) {
		postCollection(scrobblerMangaId, JSONObject().put("type", TYPE_READING))
		saveState(scrobblerMangaId, mangaId)
	}

	override suspend fun updateRate(rateId: Int, mangaId: String, chapter: Int) {
		val subjectId = rateId.toLong()
		postCollection(subjectId, JSONObject().put("ep_status", chapter))
		saveState(subjectId, mangaId)
	}

	override suspend fun updateRate(rateId: Int, mangaId: String, rating: Float, status: String?, comment: String?) {
		val subjectId = rateId.toLong()
		val payload = JSONObject().put("rate", rating.toInt().coerceIn(0, 10))
		if (status != null) payload.put("type", status.toIntOrNull() ?: TYPE_READING)
		if (comment != null) payload.put("comment", comment)
		postCollection(subjectId, payload)
		saveState(subjectId, mangaId)
	}

	// Bangumi upserts a collection by subject id; the current user is addressed
	// as `-`, and the POST answers 202/204 with no body, so we re-read the state.
	private suspend fun postCollection(subjectId: Long, payload: JSONObject) {
		val request = Request.Builder()
			.url("$API_URL/v0/users/-/collections/$subjectId")
			.post(payload.toRequestBody())
			.build()
		okHttp.newCall(request).await().close()
	}

	private suspend fun saveState(subjectId: Long, mangaId: String) {
		val request = Request.Builder().get().url("$API_URL/v0/users/-/collections/$subjectId")
		val json = okHttp.newCall(request.build()).await().parseJson()
		val entity = ScrobblingEntity(
			scrobbler = ScrobblerService.BANGUMI.id,
			id = subjectId.toInt(),
			mangaId = mangaId,
			targetId = subjectId,
			status = json.optInt("type", TYPE_READING).toString(),
			chapter = json.optInt("ep_status", 0),
			comment = json.getStringOrNull("comment").orEmpty(),
			rating = (json.optInt("rate", 0).toFloat() / 10f).coerceIn(0f, 1f),
		)
		db.getScrobblingDao().upsert(entity)
	}

	private fun ScrobblerManga(json: JSONObject, sourceTitle: String): ScrobblerManga {
		val name = json.getString("name")
		val nameCn = json.getStringOrNull("name_cn")
		return ScrobblerManga(
			id = json.getLong("id"),
			name = if (nameCn.isNullOrBlank()) name else nameCn,
			altName = name,
			cover = json.optJSONObject("images")?.let { it.getStringOrNull("common") ?: it.getStringOrNull("medium") },
			url = "$WEB_URL/subject/${json.getLong("id")}",
			isBestMatch = name.equals(sourceTitle, ignoreCase = true) ||
				nameCn?.equals(sourceTitle, ignoreCase = true) == true,
		)
	}

	private fun ScrobblerMangaInfo(json: JSONObject): ScrobblerMangaInfo {
		val name = json.getString("name")
		val nameCn = json.getStringOrNull("name_cn")
		return ScrobblerMangaInfo(
			id = json.getLong("id"),
			name = if (nameCn.isNullOrBlank()) name else nameCn,
			cover = json.optJSONObject("images")?.getStringOrNull("common").orEmpty(),
			url = "$WEB_URL/subject/${json.getLong("id")}",
			descriptionHtml = json.getStringOrNull("summary").orEmpty(),
		)
	}

	@Suppress("FunctionName")
	private fun BangumiUser(json: JSONObject) = ScrobblerUser(
		id = json.getLong("id"),
		nickname = json.getStringOrNull("nickname") ?: json.getString("username"),
		avatar = json.optJSONObject("avatar")?.getStringOrNull("large"),
		service = ScrobblerService.BANGUMI,
	)

	private companion object {
		// Bangumi collection type: 1 wish, 2 collect, 3 do(reading), 4 on_hold, 5 dropped.
		const val TYPE_READING = 3
	}
}
