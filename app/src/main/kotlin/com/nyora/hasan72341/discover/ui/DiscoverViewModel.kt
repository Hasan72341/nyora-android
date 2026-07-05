package com.nyora.hasan72341.discover.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.plus
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.model.getTitle
import com.nyora.hasan72341.core.model.toMangaSource
import com.nyora.hasan72341.core.prefs.AppSettings
import com.nyora.hasan72341.core.prefs.observeAsFlow
import com.nyora.hasan72341.core.ui.BaseViewModel
import com.nyora.hasan72341.discover.domain.DiscoverMultiRails
import com.nyora.hasan72341.discover.domain.DiscoverPopularSource
import com.nyora.hasan72341.discover.domain.DiscoverRepository
import com.nyora.hasan72341.discover.ui.model.DiscoverHeroItem
import com.nyora.hasan72341.discover.ui.model.DiscoverRail
import com.nyora.hasan72341.discover.ui.model.DiscoverRailCard
import com.nyora.hasan72341.discover.ui.model.DiscoverRailError
import com.nyora.hasan72341.discover.ui.model.DiscoverRailLoading
import com.nyora.hasan72341.discover.ui.model.DiscoverRailState
import com.nyora.hasan72341.explore.ui.model.RecommendationsItem
import com.nyora.hasan72341.history.data.HistoryRepository
import com.nyora.hasan72341.list.ui.model.EmptyHint
import com.nyora.hasan72341.list.ui.model.ListHeader
import com.nyora.hasan72341.list.ui.model.ListModel
import com.nyora.hasan72341.list.ui.model.MangaCompactListModel
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.mihon.parsers.model.MangaSource
import com.nyora.hasan72341.mihon.parsers.util.runCatchingCancellable
import com.nyora.hasan72341.suggestions.domain.SuggestionRepository
import com.nyora.hasan72341.tracker.domain.TrackingRepository
import javax.inject.Inject

@HiltViewModel
class DiscoverViewModel @Inject constructor(
	@ApplicationContext private val context: Context,
	private val historyRepository: HistoryRepository,
	private val trackingRepository: TrackingRepository,
	private val discoverRepository: DiscoverRepository,
	private val suggestionRepository: SuggestionRepository,
	private val settings: AppSettings,
) : BaseViewModel() {

	/**
	 * The source that produced the non-empty "Popular on <Source>" rail, captured during
	 * [buildList] so [DiscoverFragment]'s Show-all can deep-link into it. Volatile because it is
	 * written on the combine() Default dispatcher and read on the main thread.
	 */
	@Volatile
	var lastPopularSource: MangaSource? = null
		private set

	private val historyFlow: Flow<List<Manga>> = historyRepository.observeAll(limit = 12)

	private val updatesFlow: Flow<List<Manga>> =
		trackingRepository.observeTrackingLog(limit = 20, filterOptions = emptySet())
			.map { log -> log.map { it.manga } }

	private val retryTrigger = MutableStateFlow(0)

	/**
	 * All AniList rails from ONE multi-alias request.
	 * null  -> still loading (every AniList rail renders shimmer)
	 * value -> loaded; per-rail empty list renders an Error/hidden state.
	 */
	private val anilistFlow: Flow<DiscoverMultiRails?> = retryTrigger.flatMapLatest {
		flow {
			emit(null)
			emit(
				runCatchingCancellable { discoverRepository.getMultiRails(20) }.getOrNull()
					?: EMPTY_MULTI_RAILS,
			)
		}
	}.flowOn(Dispatchers.IO)

	/**
	 * "Popular on <Source>" from an installed source.
	 * null sentinel -> still loading (shimmer); non-null -> loaded result (empty hides the rail).
	 */
	private val popularFlow: Flow<DiscoverPopularSource?> = retryTrigger.flatMapLatest {
		flow {
			val cached = discoverRepository.cachedPopularSource
			if (cached == null) {
				emit(null)
			} else {
				emit(cached)
			}
			emit(
				runCatchingCancellable { discoverRepository.getPopularWithSource(20) }.getOrNull()
					?: DiscoverPopularSource(source = null, cards = emptyList()),
			)
		}
	}.flowOn(Dispatchers.IO)

	private val suggestionsFlow: Flow<List<Manga>> = settings.observeAsFlow(AppSettings.KEY_SUGGESTIONS) { isSuggestionsEnabled }
		.flatMapLatest { isEnabled ->
			if (isEnabled) {
				flow {
					emit(runCatchingCancellable { suggestionRepository.getRandomList(8) }.getOrDefault(emptyList()))
				}
			} else {
				flowOf(emptyList())
			}
		}.flowOn(Dispatchers.IO)

	val content: StateFlow<List<ListModel>> = combine(
		historyFlow,
		updatesFlow,
		anilistFlow,
		popularFlow,
		suggestionsFlow,
	) { history, updates, anilist, popular, suggestions ->
		buildList(history, updates, anilist, popular, suggestions)
	}.stateIn(
		viewModelScope + Dispatchers.Default,
		SharingStarted.Eagerly,
		buildList(
			history = emptyList(),
			updates = emptyList(),
			anilist = discoverRepository.cachedMultiRails,
			popular = discoverRepository.cachedPopularSource,
			suggestions = emptyList(),
		),
	)

	fun retryNetworkRails() {
		retryTrigger.update { it + 1 }
	}

	fun update() {
		discoverRepository.clearCache()
		retryNetworkRails()
	}

	private fun buildList(
		history: List<Manga>,
		updates: List<Manga>,
		anilist: DiscoverMultiRails?,
		popular: DiscoverPopularSource?,
		suggestions: List<Manga>,
	): List<ListModel> {
		val out = ArrayList<ListModel>()

		// 1. Hero — first AniList trending media.
		when {
			anilist == null -> out += DiscoverRailLoading(RAIL_HERO)
			anilist.heroSource != null -> {
				val media = anilist.heroSource
				out += DiscoverHeroItem(
					coverUrl = media.coverImage.preferred().ifBlank { null },
					title = media.title.preferred(),
					genres = media.genres.take(3).joinToString(", "),
					searchQuery = media.title.preferred(),
				)
			}
		}

		// 2. Continue reading (local, omit if empty).
		localRail(out, R.string.continue_reading, RAIL_CONTINUE_READING, history)

		// 3. New chapters (local, omit if empty).
		localRail(out, R.string.new_chapters, RAIL_UPDATES, updates)

		// 4. Suggestions
		if (suggestions.isNotEmpty()) {
			out += ListHeader(R.string.suggestions)
			out += RecommendationsItem(suggestions.map { manga ->
				MangaCompactListModel(
					manga = manga,
					override = null,
					subtitle = manga.tags.joinToString { it.title },
					counter = 0,
				)
			})
		}

		// 5. Trending now.
		networkRail(out, R.string.trending_now, RAIL_TRENDING, anilistState(anilist) { it.trending })

		// 6. Top rated.
		networkRail(out, R.string.top_rated, RAIL_TOP_RATED, anilistState(anilist) { it.topRated })

		// 7. Popular manhwa.
		networkRail(out, R.string.popular_manhwa, RAIL_MANHWA, anilistState(anilist) { it.manhwa })

		// 8. Action (genre).
		networkRail(out, R.string.genre_action, RAIL_GENRE_ACTION, anilistState(anilist) { it.action })

		// 9. Romance (genre).
		networkRail(out, R.string.genre_romance, RAIL_GENRE_ROMANCE, anilistState(anilist) { it.romance })

		// 10. Fantasy (genre).
		networkRail(out, R.string.genre_fantasy, RAIL_GENRE_FANTASY, anilistState(anilist) { it.fantasy })

		// 11. Comedy (genre).
		networkRail(out, R.string.genre_comedy, RAIL_GENRE_COMEDY, anilistState(anilist) { it.comedy })

		// 12. Popular on <Source> (dynamic, localized header).
		popularSourceRail(out, popular)

		if (out.isEmpty()) {
			out += EmptyHint(
				icon = R.drawable.ic_empty_common,
				textPrimary = R.string.text_empty_holder_primary,
				textSecondary = R.string.text_history_holder_secondary,
				actionStringRes = 0,
			)
		}
		return out
	}

	/**
	 * Maps a per-rail slice of the multi result to a [DiscoverRailState]:
	 * loading while [anilist] is null, otherwise Loaded if the list is non-empty else Error.
	 */
	private inline fun anilistState(
		anilist: DiscoverMultiRails?,
		select: (DiscoverMultiRails) -> List<DiscoverRailCard>,
	): DiscoverRailState = when {
		anilist == null -> DiscoverRailState.Loading
		else -> {
			val cards = select(anilist)
			if (cards.isNotEmpty()) DiscoverRailState.Loaded(cards) else DiscoverRailState.Error
		}
	}

	private fun popularSourceRail(out: MutableList<ListModel>, popular: DiscoverPopularSource?) {
		when {
			popular == null -> {
				out += ListHeader(textRes = R.string.popular_now, buttonTextRes = R.string.show_all, payload = RAIL_POPULAR_SOURCE)
				out += DiscoverRailLoading(RAIL_POPULAR_SOURCE)
			}

			popular.cards.isNotEmpty() -> {
				lastPopularSource = popular.source
				val headerText = popular.source
					?.let { context.getString(R.string.popular_on_source, it.getTitle(context)) }
					?: context.getString(R.string.popular_now)
				out += ListHeader(
					text = headerText,
					buttonTextRes = R.string.show_all,
					payload = RAIL_POPULAR_SOURCE,
				)
				out += DiscoverRail(railId = RAIL_POPULAR_SOURCE, items = popular.cards)
			}
			// Loaded but empty -> hide the rail entirely.
		}
	}

	private fun localRail(
		out: MutableList<ListModel>,
		@StringRes title: Int,
		railId: String,
		manga: List<Manga>,
	) {
		if (manga.isEmpty()) return
		out += ListHeader(textRes = title, buttonTextRes = R.string.show_all, payload = railId)
		out += DiscoverRail(railId = railId, items = cardsFromManga(manga))
	}

	private fun networkRail(
		out: MutableList<ListModel>,
		@StringRes title: Int,
		railId: String,
		state: DiscoverRailState,
	) {
		val header = ListHeader(textRes = title, buttonTextRes = R.string.show_all, payload = railId)
		out += header
		when (state) {
			is DiscoverRailState.Loading -> out += DiscoverRailLoading(railId)
			is DiscoverRailState.Loaded -> {
				if (state.items.isNotEmpty()) {
					out += DiscoverRail(railId = railId, items = state.items)
				} else {
					// Remove the orphan header just added.
					out.remove(header)
				}
			}

			is DiscoverRailState.Error -> out += DiscoverRailError(railId)
		}
	}

	private fun cardsFromManga(manga: List<Manga>): List<DiscoverRailCard> = manga.map {
		DiscoverRailCard(
			id = it.id.toLongOrNull() ?: it.id.hashCode().toLong(),
			title = it.title,
			coverUrl = it.coverUrl,
			source = it.source.toMangaSource(),
			manga = it,
			searchQuery = null,
		)
	}

	companion object {
		const val RAIL_HERO = "hero"
		const val RAIL_CONTINUE_READING = "continue_reading"
		const val RAIL_UPDATES = "updates"
		const val RAIL_TRENDING = "trending"
		const val RAIL_TOP_RATED = "top_rated"
		const val RAIL_MANHWA = "manhwa"
		const val RAIL_GENRE_ACTION = "genre_action"
		const val RAIL_GENRE_ROMANCE = "genre_romance"
		const val RAIL_GENRE_FANTASY = "genre_fantasy"
		const val RAIL_GENRE_COMEDY = "genre_comedy"
		const val RAIL_POPULAR_SOURCE = "popular_source"

		private val EMPTY_MULTI_RAILS = DiscoverMultiRails(
			heroSource = null,
			trending = emptyList(),
			topRated = emptyList(),
			manhwa = emptyList(),
			action = emptyList(),
			romance = emptyList(),
			fantasy = emptyList(),
			comedy = emptyList(),
		)
	}
}
