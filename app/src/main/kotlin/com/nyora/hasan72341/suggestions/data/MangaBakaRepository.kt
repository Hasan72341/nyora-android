package com.nyora.hasan72341.suggestions.data

import com.nyora.hasan72341.core.network.BaseHttpClient
import com.nyora.hasan72341.core.prefs.AppSettings
import com.nyora.hasan72341.tachiyomi.network.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// Shared discovery domain models.
//
// These were originally AniList-shaped; they are kept as the app's neutral
// discovery model so the Discover/Suggestions UI, [SmartMatchUseCase] and
// [MangaSuggestionV2] are untouched. The data now comes from MangaBaka
// (see [MangaBakaRepository]); the AniList *tracker* is a separate feature and
// is unaffected.
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

// ---------------------------------------------------------------------------
// MangaBaka wire models (https://api.mangabaka.dev/v1/series/search).
// ---------------------------------------------------------------------------

@Serializable
private data class MangaBakaSearchResponse(val data: List<MangaBakaSeries> = emptyList())

@Serializable
private data class MangaBakaSeries(
	val id: Int? = null,
	val title: String? = null,
	val romanized_title: String? = null,
	val native_title: String? = null,
	val cover: MangaBakaCover? = null,
	val genres: List<String> = emptyList(),
	val rating: Double? = null,
	val popularity: MangaBakaPopularity? = null,
	val type: String? = null,
	val state: String? = null,
	val description: String? = null,
)

@Serializable
private data class MangaBakaCover(
	val x350: MangaBakaCoverVariant? = null,
	val x250: MangaBakaCoverVariant? = null,
	val raw: MangaBakaCoverRaw? = null,
)

@Serializable
private data class MangaBakaCoverVariant(val x1: String? = null)

@Serializable
private data class MangaBakaCoverRaw(val url: String? = null)

@Serializable
private data class MangaBakaPopularity(val global: MangaBakaPopularityGlobal? = null)

@Serializable
private data class MangaBakaPopularityGlobal(val current: Double? = null)

/** All Discover rails fetched from MangaBaka in one shot. Any rail may be empty on failure. */
data class MangaBakaRails(
	val trending: List<AnilistMedia>,
	val topRated: List<AnilistMedia>,
	val manhwa: List<AnilistMedia>,
	val action: List<AnilistMedia>,
	val romance: List<AnilistMedia>,
	val fantasy: List<AnilistMedia>,
	val comedy: List<AnilistMedia>,
)

/**
 * Discovery/Suggestions data source backed by the MangaBaka database.
 *
 * MangaBaka's search API is search-first: there is NO trending/popularity endpoint and
 * `sort=` is rejected, so every rail is a broad (or genre-filtered) search that we rank
 * and filter CLIENT-SIDE — by `popularity.global.current` (desc) or `rating` (desc),
 * dropping merged/cover-less/too-short junk. Results are mapped onto the app's neutral
 * [AnilistMedia] discovery model so the UI is unchanged.
 *
 * Content safety follows [AppSettings.isNsfwContentDisabled]: safe-only by default.
 */
@Singleton
class MangaBakaRepository @Inject constructor(
	@BaseHttpClient private val okHttpClient: OkHttpClient,
	private val settings: AppSettings,
) {
	private val json = Json { ignoreUnknownKeys = true }

	private enum class SortBy { POPULARITY, RATING }

	// --- public API (mirrors the old AniList repository signatures) --------

	/** Broad manga search ranked by popularity — drives the Suggestions "trending" list. */
	suspend fun getTrendingManga(limit: Int = 20): List<AnilistMedia> =
		fetchRail(sortBy = SortBy.POPULARITY, take = limit)

	/** Broad manga search ranked by rating — combined with trending for Suggestions variety. */
	suspend fun getPopularManga(limit: Int = 20): List<AnilistMedia> =
		fetchRail(sortBy = SortBy.RATING, take = limit)

	/**
	 * All Discover rails. Each rail is fetched independently; a single failure yields an
	 * empty list for that rail rather than failing the whole page. Returns null only if
	 * every rail failed (so callers can distinguish "no data" from "loading").
	 */
	suspend fun getMultiRails(limit: Int = 20): MangaBakaRails? {
		val rails = MangaBakaRails(
			trending = safeRail { fetchRail(sortBy = SortBy.POPULARITY, take = limit) },
			topRated = safeRail { fetchRail(sortBy = SortBy.RATING, take = limit) },
			manhwa = safeRail { fetchRail(type = "manhwa", sortBy = SortBy.POPULARITY, take = limit) },
			action = safeRail { fetchRail(genre = "Action", sortBy = SortBy.POPULARITY, take = limit) },
			romance = safeRail { fetchRail(genre = "Romance", sortBy = SortBy.POPULARITY, take = limit) },
			fantasy = safeRail { fetchRail(genre = "Fantasy", sortBy = SortBy.POPULARITY, take = limit) },
			comedy = safeRail { fetchRail(genre = "Comedy", sortBy = SortBy.POPULARITY, take = limit) },
		)
		val anyContent = listOf(
			rails.trending, rails.topRated, rails.manhwa,
			rails.action, rails.romance, rails.fantasy, rails.comedy,
		).any { it.isNotEmpty() }
		return if (anyContent) rails else null
	}

	// --- internals ---------------------------------------------------------

	private inline fun safeRail(block: () -> List<AnilistMedia>): List<AnilistMedia> =
		try {
			block()
		} catch (e: Exception) {
			emptyList()
		}

	/**
	 * One MangaBaka search -> a normalized, quality-filtered, client-side-sorted rail.
	 * `q` is required by the API (there is no empty-browse), so we pass a broad token.
	 */
	private suspend fun fetchRail(
		genre: String? = null,
		type: String = "manga",
		sortBy: SortBy = SortBy.POPULARITY,
		limit: Int = 30,
		take: Int = 20,
	): List<AnilistMedia> {
		val urlBuilder = SEARCH_URL.toHttpUrl().newBuilder()
			.addQueryParameter("q", BROAD_QUERY)
			.addQueryParameter("type", type)
			.addQueryParameter("limit", limit.toString())
		if (settings.isNsfwContentDisabled) {
			urlBuilder.addQueryParameter("content_rating", "safe")
		}
		if (genre != null) {
			urlBuilder.addQueryParameter("genre", genre)
		}

		val request = Request.Builder()
			.url(urlBuilder.build())
			.header("Accept", "application/json")
			.get()
			.build()

		val series = okHttpClient.newCall(request).await().use { response ->
			if (!response.isSuccessful) return@use emptyList<MangaBakaSeries>()
			val body = response.body?.string() ?: return@use emptyList<MangaBakaSeries>()
			json.decodeFromString<MangaBakaSearchResponse>(body).data
		}

		return series
			.filter { it.isUsable() }
			.sortedByDescending { if (sortBy == SortBy.RATING) it.rating ?: 0.0 else it.popularityScore() }
			.take(take)
			.map { it.toAnilistMedia() }
	}

	private fun MangaBakaSeries.isUsable(): Boolean =
		state != "merged" &&
			bestCover().isNotBlank() &&
			(title ?: "").trim().length >= 3

	private fun MangaBakaSeries.popularityScore(): Double = popularity?.global?.current ?: 0.0

	private fun MangaBakaSeries.bestCover(): String =
		cover?.x350?.x1 ?: cover?.x250?.x1 ?: cover?.raw?.url ?: ""

	private fun MangaBakaSeries.toAnilistMedia(): AnilistMedia {
		val bestTitle = title ?: romanized_title ?: native_title ?: "Untitled"
		val coverUrl = bestCover()
		return AnilistMedia(
			id = id ?: bestTitle.hashCode(),
			title = AnilistTitle(
				romaji = romanized_title ?: bestTitle,
				english = title ?: bestTitle,
				native = native_title,
			),
			coverImage = AnilistCoverImage(extraLarge = coverUrl, large = coverUrl),
			description = description,
			averageScore = rating?.roundToInt(),
			genres = genres.map(::prettyGenre).take(6),
		)
	}

	/** "school_life" -> "School Life" */
	private fun prettyGenre(raw: String): String =
		raw.split('_').joinToString(" ") { part ->
			part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
		}

	private companion object {
		const val SEARCH_URL = "https://api.mangabaka.dev/v1/series/search"

		// MangaBaka search requires `q`; this broad token matches the bulk of the
		// catalogue, which we then rank ourselves.
		const val BROAD_QUERY = "a"
	}
}
