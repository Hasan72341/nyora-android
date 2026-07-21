package com.nyora.hasan72341.suggestions.data

import com.nyora.hasan72341.core.network.BaseHttpClient
import com.nyora.hasan72341.tachiyomi.network.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

// ---------------------------------------------------------------------------
// Shared discovery model (AniList media shape). Consumed by the Discover feed,
// the Suggestions grid, SmartMatchUseCase and MangaSuggestionV2.
// ---------------------------------------------------------------------------

@Serializable
data class AnilistMedia(
	val id: Int,
	val title: AnilistTitle,
	val coverImage: AnilistCoverImage,
	val description: String? = null,
	val averageScore: Int? = null,
	val genres: List<String> = emptyList(),
)

@Serializable
data class AnilistTitle(val romaji: String? = null, val english: String? = null, val native: String? = null) {
	fun preferred(): String = english ?: romaji ?: native ?: "Unknown"
}

@Serializable
data class AnilistCoverImage(val extraLarge: String? = null, val large: String? = null) {
	fun preferred(): String = extraLarge ?: large ?: ""
}

/** All Discover rails from one aliased AniList query. Any rail may be empty on failure. */
data class AnilistRails(
	val trending: List<AnilistMedia>,
	val manhwa: List<AnilistMedia>,
	val manhua: List<AnilistMedia>,
	val manga: List<AnilistMedia>,
	val action: List<AnilistMedia>,
	val romance: List<AnilistMedia>,
	val fantasy: List<AnilistMedia>,
)

// --- wire models ----------------------------------------------------------

@Serializable
private data class GraphQLRequest(val query: String)

@Serializable
private data class AnilistResponse(val data: AnilistData? = null)

@Serializable
private data class AnilistData(val Page: AnilistPage? = null)

@Serializable
private data class AnilistMultiResponse(val data: AnilistMultiData? = null)

@Serializable
private data class AnilistMultiData(
	val trending: AnilistPage? = null,
	val manhwa: AnilistPage? = null,
	val manhua: AnilistPage? = null,
	val manga: AnilistPage? = null,
	val action: AnilistPage? = null,
	val romance: AnilistPage? = null,
	val fantasy: AnilistPage? = null,
)

@Serializable
private data class AnilistPage(val media: List<AnilistMedia> = emptyList())

/**
 * Discover/Suggestions feed backed by AniList's public GraphQL API (no auth).
 *
 * Mirrors nyora-web's `core/discover-feed.js`: one POST with aliased Page queries,
 * `countryOfOrigin` splitting the MANGA type into manga (JP) / manhwa (KR) /
 * manhua (CN), plus genre rails, all `isAdult: false`. Entries without a cover are
 * dropped at the source. The AniList *account tracker* is a separate feature.
 */
@Singleton
class AnilistRepository @Inject constructor(
	@BaseHttpClient private val okHttpClient: OkHttpClient,
) {
	private val json = Json { ignoreUnknownKeys = true }

	/** Broad trending manga — drives the Suggestions "trending" list. */
	suspend fun getTrendingManga(limit: Int = 20): List<AnilistMedia> =
		executeSingle(singleQuery(filter = "", sort = "TRENDING_DESC", perPage = limit))

	/** Broad popular manga — combined with trending for Suggestions variety. */
	suspend fun getPopularManga(limit: Int = 20): List<AnilistMedia> =
		executeSingle(singleQuery(filter = "", sort = "POPULARITY_DESC", perPage = limit))

	/**
	 * All Discover rails in one request. Returns null only if the whole call failed
	 * (so callers can tell "no data" from "loading"); individual rails may be empty.
	 */
	suspend fun getMultiRails(limit: Int = 30): AnilistRails? {
		val query = buildString {
			append("query { ")
			append(aliasedPage("trending", "", "TRENDING_DESC", limit))
			append(aliasedPage("manhwa", "countryOfOrigin: KR, ", "POPULARITY_DESC", limit))
			append(aliasedPage("manhua", "countryOfOrigin: CN, ", "POPULARITY_DESC", limit))
			append(aliasedPage("manga", "countryOfOrigin: JP, ", "POPULARITY_DESC", limit))
			append(aliasedPage("action", "genre: \"Action\", ", "TRENDING_DESC", limit))
			append(aliasedPage("romance", "genre: \"Romance\", ", "TRENDING_DESC", limit))
			append(aliasedPage("fantasy", "genre: \"Fantasy\", ", "TRENDING_DESC", limit))
			append("}")
		}
		val body = post(query) ?: return null
		val data = runCatching { json.decodeFromString<AnilistMultiResponse>(body).data }.getOrNull() ?: return null
		val rails = AnilistRails(
			trending = data.trending.usable(),
			manhwa = data.manhwa.usable(),
			manhua = data.manhua.usable(),
			manga = data.manga.usable(),
			action = data.action.usable(),
			romance = data.romance.usable(),
			fantasy = data.fantasy.usable(),
		)
		val anyContent = listOf(
			rails.trending, rails.manhwa, rails.manhua, rails.manga,
			rails.action, rails.romance, rails.fantasy,
		).any { it.isNotEmpty() }
		return if (anyContent) rails else null
	}

	private suspend fun executeSingle(query: String): List<AnilistMedia> {
		val body = post(query) ?: return emptyList()
		return runCatching { json.decodeFromString<AnilistResponse>(body).data?.Page }.getOrNull().usable()
	}

	private suspend fun post(query: String): String? {
		val requestBody = json.encodeToString(GraphQLRequest.serializer(), GraphQLRequest(query))
			.toRequestBody(JSON_MEDIA_TYPE)
		val request = Request.Builder()
			.url(ANILIST_URL)
			.header("Accept", "application/json")
			.post(requestBody)
			.build()
		return okHttpClient.newCall(request).await().use { response ->
			if (!response.isSuccessful) null else response.body?.string()
		}
	}

	// Renderers need a real cover, so entries without one are dropped.
	private fun AnilistPage?.usable(): List<AnilistMedia> =
		this?.media?.filter { it.coverImage.extraLarge != null || it.coverImage.large != null }.orEmpty()

	private fun aliasedPage(alias: String, filter: String, sort: String, perPage: Int): String =
		"$alias: Page(perPage: $perPage) { media(type: MANGA, isAdult: false, ${filter}sort: $sort) { $MEDIA_FIELDS } } "

	private fun singleQuery(filter: String, sort: String, perPage: Int): String =
		"query { Page(perPage: $perPage) { media(type: MANGA, isAdult: false, ${filter}sort: $sort) { $MEDIA_FIELDS } } }"

	private companion object {
		const val ANILIST_URL = "https://graphql.anilist.co"
		const val MEDIA_FIELDS =
			"id title { romaji english native } coverImage { large extraLarge } genres averageScore"
		val JSON_MEDIA_TYPE = "application/json".toMediaType()
	}
}
