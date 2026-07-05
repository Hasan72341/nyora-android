package com.nyora.hasan72341.discover.domain

import com.nyora.hasan72341.core.model.toMangaSource
import com.nyora.hasan72341.core.parser.MangaRepository
import com.nyora.hasan72341.explore.data.MangaSourcesRepository
import com.nyora.hasan72341.discover.ui.model.DiscoverRailCard
import com.nyora.hasan72341.suggestions.data.AnilistMedia
import com.nyora.hasan72341.suggestions.data.MangaBakaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.nyora.hasan72341.mihon.parsers.model.MangaListFilter
import com.nyora.hasan72341.mihon.parsers.model.MangaSource
import com.nyora.hasan72341.mihon.parsers.model.SortOrder
import com.nyora.hasan72341.mihon.parsers.util.runCatchingCancellable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * All MangaBaka-backed network rails for the Discover page (see
 * [MangaBakaRepository.getMultiRails]). MangaBaka is search-first with no sort endpoint,
 * so each rail is a broad/genre search ranked client-side by popularity or rating.
 *
 * [heroSource] is the raw first trending media (it carries genres, which [DiscoverRailCard]
 * does not) so the VM can build the featured hero from it. Every list is already mapped
 * to the renderable [DiscoverRailCard]; on failure each list is simply empty.
 */
data class DiscoverMultiRails(
	val heroSource: AnilistMedia?,
	val trending: List<DiscoverRailCard>,
	val topRated: List<DiscoverRailCard>,
	val manhwa: List<DiscoverRailCard>,
	val action: List<DiscoverRailCard>,
	val romance: List<DiscoverRailCard>,
	val fantasy: List<DiscoverRailCard>,
	val comedy: List<DiscoverRailCard>,
)

/**
 * "Popular on <Source>" rail. [source] is the installed source that actually produced
 * the non-empty [cards] (used for the header title + the Show-all deep link). Both are
 * null/empty on total failure.
 */
data class DiscoverPopularSource(
	val source: MangaSource?,
	val cards: List<DiscoverRailCard>,
)

/**
 * Aggregates the always-populated network rails for the Discover landing page:
 *  - MangaBaka rails (trending / top rated / popular manhwa / genre rails) fetched as
 *    parallel searches and ranked client-side, plus the hero source.
 *  - "Popular on <Source>" from a sensible installed/pinned source.
 *
 * Every public method runs its network work on [Dispatchers.IO] and never throws:
 * on any failure it returns empty lists so a single failing rail can never take down
 * the rest of the page.
 */
@Singleton
class DiscoverRepository @Inject constructor(
	private val mangaBakaRepository: MangaBakaRepository,
	private val sourcesRepository: MangaSourcesRepository,
	private val mangaRepositoryFactory: MangaRepository.Factory,
) {

	var cachedMultiRails: DiscoverMultiRails? = null
		private set
	var cachedPopularSource: DiscoverPopularSource? = null
		private set

	/**
	 * Maps a single AniList media to a renderable [DiscoverRailCard]. Ids are namespaced
	 * negative to avoid colliding with real manga ids; coverUrl/manga/source are
	 * AniList-only (no real Manga), so tapping a card routes to search via [searchQuery].
	 */
	private fun toCard(media: AnilistMedia): DiscoverRailCard = DiscoverRailCard(
		id = -(media.id.toLong()),
		title = media.title.preferred(),
		coverUrl = media.coverImage.preferred().ifBlank { null },
		source = null,
		manga = null,
		searchQuery = media.title.preferred(),
	)

	/**
	 * All MangaBaka rails via parallel searches. Graceful: on any failure (network/decode)
	 * every list is empty and [heroSource] is null, so the VM renders each rail as
	 * Error/hidden rather than crashing.
	 */
	suspend fun getMultiRails(limit: Int = 20): DiscoverMultiRails = withContext(Dispatchers.IO) {
		val cached = cachedMultiRails
		if (cached != null && cached.trending.isNotEmpty()) {
			return@withContext cached
		}
		val response = runCatchingCancellable { mangaBakaRepository.getMultiRails(limit) }.getOrNull()
		val result = DiscoverMultiRails(
			heroSource = response?.trending?.firstOrNull(),
			trending = response?.trending?.map(::toCard).orEmpty(),
			topRated = response?.topRated?.map(::toCard).orEmpty(),
			manhwa = response?.manhwa?.map(::toCard).orEmpty(),
			action = response?.action?.map(::toCard).orEmpty(),
			romance = response?.romance?.map(::toCard).orEmpty(),
			fantasy = response?.fantasy?.map(::toCard).orEmpty(),
			comedy = response?.comedy?.map(::toCard).orEmpty(),
		)
		if (result.trending.isNotEmpty()) {
			cachedMultiRails = result
		}
		result
	}

	/**
	 * "Popular on <Source>" from an installed source. Picks a sensible default source,
	 * fetches its popular list, and on empty/failure retries with up to 2 more enabled
	 * sources. Returns the source that produced the non-empty list so the VM can build
	 * the header title and Show-all deep link; on total failure returns null source +
	 * empty cards.
	 */
	suspend fun getPopularWithSource(limit: Int = 20): DiscoverPopularSource = withContext(Dispatchers.IO) {
		val cached = cachedPopularSource
		if (cached != null && cached.cards.isNotEmpty()) {
			return@withContext cached
		}
		val primary = runCatchingCancellable {
			sourcesRepository.getTopSources(6).firstOrNull()
				?: sourcesRepository.getPinnedSources().firstOrNull()
				?: sourcesRepository.getEnabledSources().firstOrNull()
		}.getOrNull()

		val tried = LinkedHashSet<MangaSource>()

		primary?.let { source ->
			val cards = fetchPopular(source, limit)
			if (cards.isNotEmpty()) {
				val result = DiscoverPopularSource(source, cards)
				cachedPopularSource = result
				return@withContext result
			}
			tried.add(source)
		}

		// One-retry loop: try up to 2 more enabled sources we have not already tried.
		val fallback = runCatchingCancellable {
			sourcesRepository.getEnabledSources()
		}.getOrDefault(emptyList())
		for (source in fallback) {
			if (source in tried) continue
			tried.add(source)
			val cards = fetchPopular(source, limit)
			if (cards.isNotEmpty()) {
				val result = DiscoverPopularSource(source, cards)
				cachedPopularSource = result
				return@withContext result
			}
			if (tried.size >= 3) break
		}
		DiscoverPopularSource(null, emptyList())
	}

	private suspend fun fetchPopular(source: MangaSource, limit: Int): List<DiscoverRailCard> =
		runCatchingCancellable {
			val repo = mangaRepositoryFactory.create(source)
			val order = if (SortOrder.POPULARITY in repo.sortOrders) {
				SortOrder.POPULARITY
			} else {
				repo.defaultSortOrder
			}
			repo.getList(offset = 0, order = order, filter = MangaListFilter.EMPTY)
				.take(limit)
				.map { manga ->
					DiscoverRailCard(
						id = manga.id.hashCode().toLong(),
						title = manga.title,
						coverUrl = manga.coverUrl,
						source = manga.source.toMangaSource(),
						manga = manga,
						searchQuery = null,
					)
				}
		}.getOrDefault(emptyList())

	fun clearCache() {
		cachedMultiRails = null
		cachedPopularSource = null
	}
}

