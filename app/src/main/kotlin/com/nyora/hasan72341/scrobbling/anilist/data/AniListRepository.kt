package com.nyora.hasan72341.scrobbling.anilist.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.db.MangaDatabase
import com.nyora.hasan72341.mihon.parsers.exception.GraphQLException
import com.nyora.hasan72341.tachiyomi.network.await
import com.nyora.hasan72341.mihon.parsers.util.json.getStringOrNull
import com.nyora.hasan72341.mihon.parsers.util.json.mapJSON
import com.nyora.hasan72341.mihon.parsers.util.parseJson
import com.nyora.hasan72341.mihon.parsers.util.toIntUp
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
import kotlin.math.roundToInt

private const val REDIRECT_URI = "nyora://anilist-auth"
private const val BASE_URL = "https://anilist.co/api/v2/"
private const val ENDPOINT = "https://graphql.anilist.co"
private const val MANGA_PAGE_SIZE = 10
private const val REQUEST_QUERY = "query"
private const val REQUEST_MUTATION = "mutation"
private const val KEY_SCORE_FORMAT = "score_format"

@Singleton
class AniListRepository @Inject constructor(
	@ApplicationContext context: Context,
	@ScrobblerType(ScrobblerService.ANILIST) private val okHttp: OkHttpClient,
	@ScrobblerType(ScrobblerService.ANILIST) private val storage: ScrobblerStorage,
	private val db: MangaDatabase,
) : ScrobblerRepository {

	private val clientId = context.getString(R.string.anilist_clientId)
	private val clientSecret = context.getString(R.string.anilist_clientSecret)

	override val oauthUrl: String
		get() = "${BASE_URL}oauth/authorize?client_id=$clientId&" +
			"redirect_uri=${REDIRECT_URI}&response_type=code"

	override val isAuthorized: Boolean
		get() = storage.accessToken != null

	private val shrinkRegex = Regex("\\t+")

	override suspend fun authorize(code: String?) {
		// AniList's registered client (46413) is a confidential authorization-code
		// app: it issues a client secret and rejects the implicit grant with
		// "unsupported_grant_type". Exchange the `code` from the redirect for a
		// token. A null code (silent refresh) can't be honoured here, so the user
		// re-authenticates.
		val authCode = requireNotNull(code) {
			"AniList sign-in returned no authorization code; sign in again"
		}
		val body = FormBody.Builder()
			.add("client_id", clientId)
			.add("client_secret", clientSecret)
			.add("grant_type", "authorization_code")
			.add("redirect_uri", REDIRECT_URI)
			.add("code", authCode)
			.build()
		val request = Request.Builder()
			.post(body)
			.url("${BASE_URL}oauth/token")
		val response = okHttp.newCall(request.build()).await().parseJson()
		storage.accessToken = response.getString("access_token")
		storage.refreshToken = response.getStringOrNull("refresh_token")
	}

	override suspend fun loadUser(): ScrobblerUser {
		val response = doRequest(
			REQUEST_QUERY,
			"""
			AniChartUser {
				user {
					id
					name
					avatar {
						medium
					}
					mediaListOptions {
						scoreFormat
					}
				}
			}
		""",
		)
		val jo = response.getJSONObject("data").getJSONObject("AniChartUser").getJSONObject("user")
		storage[KEY_SCORE_FORMAT] = jo.getJSONObject("mediaListOptions").getString("scoreFormat")
		return AniListUser(jo).also { storage.user = it }
	}

	override val cachedUser: ScrobblerUser?
		get() {
			return storage.user
		}

	override suspend fun unregister(mangaId: String) {
		return db.getScrobblingDao().delete(ScrobblerService.ANILIST.id, mangaId)
	}

	override fun logout() {
		storage.clear()
	}

	override suspend fun findManga(query: String, offset: Int): List<ScrobblerManga> {
		val page = (offset / MANGA_PAGE_SIZE.toFloat()).toIntUp() + 1
		val response = doRequest(
			REQUEST_QUERY,
			"""
			Page(page: $page, perPage: ${MANGA_PAGE_SIZE}) {
				media(type: MANGA, sort: SEARCH_MATCH, search: ${JSONObject.quote(query)}) {
					id
					title {
						userPreferred
						native
					}
					coverImage {
						medium
					}
					siteUrl
				}
			}
		""",
		)
		val data = response.getJSONObject("data").getJSONObject("Page").getJSONArray("media")
		return data.mapJSON { ScrobblerManga(it, query) }
	}

	override suspend fun createRate(mangaId: String, scrobblerMangaId: Long) {
		val response = doRequest(
			REQUEST_MUTATION,
			"""
				SaveMediaListEntry(mediaId: $scrobblerMangaId) {
					id
					mediaId
					status
					notes
					score
					progress
				}
			""",
		)
		saveRate(response.getJSONObject("data").getJSONObject("SaveMediaListEntry"), mangaId)
	}

	override suspend fun updateRate(rateId: Int, mangaId: String, chapter: Int) {
		val response = doRequest(
			REQUEST_MUTATION,
			"""
				SaveMediaListEntry(id: $rateId, progress: $chapter) {
					id
					mediaId
					status
					notes
					score
					progress
				}
			""",
		)
		saveRate(response.getJSONObject("data").getJSONObject("SaveMediaListEntry"), mangaId)
	}

	override suspend fun updateRate(rateId: Int, mangaId: String, rating: Float, status: String?, comment: String?) {
		val scoreRaw = (rating * 100f).roundToInt()
		val statusString = status?.let { ", status: $it" }.orEmpty()
		val notesString = comment?.let { ", notes: ${JSONObject.quote(it)}" }.orEmpty()
		val response = doRequest(
			REQUEST_MUTATION,
			"""
				SaveMediaListEntry(id: $rateId, scoreRaw: $scoreRaw$statusString$notesString) {
					id
					mediaId
					status
					notes
					score
					progress
				}
			""",
		)
		saveRate(response.getJSONObject("data").getJSONObject("SaveMediaListEntry"), mangaId)
	}

	override suspend fun getMangaInfo(id: Long): ScrobblerMangaInfo {
		val response = doRequest(
			REQUEST_QUERY,
			"""
			Media(id: $id) {
				id
				title {
					userPreferred
				}
				coverImage {
					large
				}
				description
				siteUrl
			}
			""",
		)
		return ScrobblerMangaInfo(response.getJSONObject("data").getJSONObject("Media"))
	}

	private suspend fun saveRate(json: JSONObject, mangaId: String) {
		val scoreFormat = ScoreFormat.of(storage[KEY_SCORE_FORMAT])
		val entity = ScrobblingEntity(
			scrobbler = ScrobblerService.ANILIST.id,
			id = json.getInt("id"),
			mangaId = mangaId,
			targetId = json.getLong("mediaId"),
			status = json.getString("status"),
			chapter = json.getInt("progress"),
			comment = json.getString("notes"),
			rating = scoreFormat.normalize(json.getDouble("score").toFloat()),
		)
		db.getScrobblingDao().upsert(entity)
	}

	private fun ScrobblerManga(json: JSONObject, sourceTitle: String): ScrobblerManga {
		val title = json.getJSONObject("title")
		return ScrobblerManga(
			id = json.getLong("id"),
			name = title.getString("userPreferred"),
			altName = title.getStringOrNull("native"),
			cover = json.getJSONObject("coverImage").getString("medium"),
			url = json.getString("siteUrl"),
			isBestMatch = sourceTitle.let {
				title.keys().forEach { key ->
					if (title.getStringOrNull(key)?.equals(it, ignoreCase = true) == true) {
						return@let true
					}
				}
				false
			},
		)
	}

	private fun ScrobblerMangaInfo(json: JSONObject) = ScrobblerMangaInfo(
		id = json.getLong("id"),
		name = json.getJSONObject("title").getString("userPreferred"),
		cover = json.getJSONObject("coverImage").getString("large"),
		url = json.getString("siteUrl"),
		descriptionHtml = json.getString("description"),
	)

	@Suppress("FunctionName")
	private fun AniListUser(json: JSONObject) = ScrobblerUser(
		id = json.getLong("id"),
		nickname = json.getString("name"),
		avatar = json.getJSONObject("avatar").getStringOrNull("medium"),
		service = ScrobblerService.ANILIST,
	)

	@OptIn(com.nyora.hasan72341.mihon.parsers.InternalParsersApi::class)
	private suspend fun doRequest(type: String, payload: String): JSONObject {
		val body = JSONObject()
		body.put("query", "$type { ${payload.shrink()} }")
		val mediaType = "application/json; charset=utf-8".toMediaType()
		val requestBody = body.toString().toRequestBody(mediaType)
		val request = Request.Builder()
			.post(requestBody)
			.url(ENDPOINT)
		val json = okHttp.newCall(request.build()).await().parseJson()
		json.optJSONArray("errors")?.let {
			if (it.length() != 0) {
				throw GraphQLException(it)
			}
		}
		return json
	}

	private fun String.shrink() = replace(shrinkRegex, " ")
}
