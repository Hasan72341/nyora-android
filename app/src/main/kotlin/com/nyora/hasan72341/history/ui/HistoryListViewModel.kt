package com.nyora.hasan72341.history.ui

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.model.MangaHistory
import com.nyora.hasan72341.core.parser.MangaDataRepository
import com.nyora.hasan72341.core.prefs.AppSettings
import com.nyora.hasan72341.core.prefs.ListMode
import com.nyora.hasan72341.core.prefs.observeAsFlow
import com.nyora.hasan72341.core.prefs.observeAsStateFlow
import com.nyora.hasan72341.core.ui.util.ReversibleAction
import com.nyora.hasan72341.core.util.ext.calculateTimeAgo
import com.nyora.hasan72341.core.util.ext.call
import com.nyora.hasan72341.core.util.ext.flattenLatest
import com.nyora.hasan72341.history.data.HistoryRepository
import com.nyora.hasan72341.history.domain.HistoryListQuickFilter
import com.nyora.hasan72341.history.domain.MarkAsReadUseCase
import com.nyora.hasan72341.history.domain.model.MangaWithHistory
import com.nyora.hasan72341.list.domain.ListFilterOption
import com.nyora.hasan72341.list.domain.ListSortOrder
import com.nyora.hasan72341.list.domain.MangaListMapper
import com.nyora.hasan72341.list.domain.QuickFilterListener
import com.nyora.hasan72341.list.domain.ReadingProgress
import com.nyora.hasan72341.list.ui.MangaListViewModel
import com.nyora.hasan72341.list.ui.model.EmptyState
import com.nyora.hasan72341.list.ui.model.InfoModel
import com.nyora.hasan72341.list.ui.model.ListHeader
import com.nyora.hasan72341.list.ui.model.ListModel
import com.nyora.hasan72341.list.ui.model.LoadingState
import com.nyora.hasan72341.list.ui.model.toErrorState
import com.nyora.hasan72341.mihon.parsers.model.Manga
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import com.nyora.hasan72341.local.data.LocalStorageChanges
import com.nyora.hasan72341.local.domain.model.LocalManga
import kotlinx.coroutines.flow.SharedFlow

import com.nyora.hasan72341.history.domain.HistoryStatsUseCase
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import com.nyora.hasan72341.history.domain.model.HistoryStats

private const val PAGE_SIZE = 16

@HiltViewModel
class HistoryListViewModel @Inject constructor(
	private val repository: HistoryRepository,
	settings: AppSettings,
	private val mangaListMapper: MangaListMapper,
	private val markAsReadUseCase: MarkAsReadUseCase,
	private val quickFilter: HistoryListQuickFilter,
	mangaDataRepository: MangaDataRepository,
	@LocalStorageChanges localStorageChanges: SharedFlow<LocalManga?>,
) : MangaListViewModel(settings, mangaDataRepository, localStorageChanges), QuickFilterListener by quickFilter {

	private val sortOrder: StateFlow<ListSortOrder> = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.IO,
		key = AppSettings.KEY_HISTORY_ORDER,
		valueProducer = { historySortOrder },
	)

	override val listMode = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.IO,
		key = AppSettings.KEY_LIST_MODE_HISTORY,
		valueProducer = { historyListMode },
	)

	private val isGroupingEnabled = settings.observeAsFlow(
		key = AppSettings.KEY_HISTORY_GROUPING,
		valueProducer = { isHistoryGroupingEnabled },
	).combine(sortOrder) { g, s ->
		g && s.isGroupingSupported()
	}

	private val limit = MutableStateFlow(PAGE_SIZE)
	private val isPaginationReady = AtomicBoolean(false)

	val isStatsEnabled = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.IO,
		key = AppSettings.KEY_STATS_ENABLED,
		valueProducer = { isStatsEnabled },
	)

	override val content = combine(
		quickFilter.appliedOptions,
		observeHistory(),
		isGroupingEnabled,
		observeListModeWithTriggers(),
		settings.observeAsFlow(AppSettings.KEY_INCOGNITO_MODE) { isIncognitoModeEnabled },
	) { filters, list, grouped, mode, incognito ->
		mapList(list, grouped, mode, filters, incognito)
	}.distinctUntilChanged().onEach {
		isPaginationReady.set(true)
	}.catch { e ->
		emit(listOf(e.toErrorState(canRetry = false)))
	}.stateIn(viewModelScope + Dispatchers.IO, SharingStarted.Eagerly, listOf(LoadingState))

	override fun onRefresh() = Unit

	override fun onRetry() = Unit

	fun clearHistory(minDate: Instant?) {
		launchJob(Dispatchers.IO) {
			val stringRes = if (minDate == null) {
				repository.clear()
				R.string.history_cleared
			} else {
				repository.deleteAfter(minDate.toEpochMilli())
				R.string.removed_from_history
			}
			onActionDone.call(ReversibleAction(stringRes, null))
		}
	}

	fun removeNotFavorite() {
		launchJob(Dispatchers.IO) {
			repository.deleteNotFavorite()
			onActionDone.call(ReversibleAction(R.string.removed_from_history, null))
		}
	}

	fun removeFromHistory(ids: Set<String>) {
		if (ids.isEmpty()) {
			return
		}
		launchJob(Dispatchers.IO) {
			val handle = repository.delete(ids.map { it.toString() })
			onActionDone.call(ReversibleAction(R.string.removed_from_history, handle))
		}
	}

	fun markAsRead(items: Set<Manga>) {
		launchLoadingJob(Dispatchers.IO) {
			markAsReadUseCase(items)
		}
	}

	fun requestMoreItems() {
		if (isPaginationReady.compareAndSet(true, false)) {
			limit.value += PAGE_SIZE
		}
	}

	private fun observeHistory() = combine(
		sortOrder,
		quickFilter.appliedOptions.combineWithSettings(),
		limit,
	) { order, filters, limit ->
		isPaginationReady.set(false)
		repository.observeAllWithHistory(order, filters, limit)
	}.flattenLatest()

	private suspend fun mapList(
		list: List<MangaWithHistory>,
		grouped: Boolean,
		mode: ListMode,
		filters: Set<ListFilterOption>,
		isIncognito: Boolean,
	): List<ListModel> {
		if (list.isEmpty()) {
			return if (filters.isEmpty()) {
				listOf(getEmptyState(hasFilters = false))
			} else {
				listOfNotNull(quickFilter.filterItem(filters), getEmptyState(hasFilters = true))
			}
		}
		val result = ArrayList<ListModel>((if (grouped) (list.size * 1.4).toInt() else list.size) + 3)
		quickFilter.filterItem(filters)?.let(result::add)
		if (isIncognito) {
			result += InfoModel(
				key = AppSettings.KEY_INCOGNITO_MODE,
				title = R.string.incognito_mode,
				text = R.string.incognito_mode_hint,
				icon = R.drawable.ic_incognito,
			)
		}
		val order = sortOrder.value
		var prevHeader: ListHeader? = null
		var isEmpty = true
		for ((manga, history) in list) {
			isEmpty = false
			if (grouped) {
				val header = history.header(order)
				if (header != prevHeader) {
					if (header != null) {
						result += header
					}
					prevHeader = header
				}
			}
			result += mangaListMapper.toListModel(manga, mode)
		}
		if (filters.isNotEmpty() && isEmpty) {
			result += getEmptyState(hasFilters = true)
		}
		return result
	}

	private fun MangaHistory.header(order: ListSortOrder): ListHeader? = when (order) {
		ListSortOrder.LAST_READ,
		ListSortOrder.LONG_AGO_READ -> calculateTimeAgo(updatedAt)?.let {
			ListHeader(it)
		} ?: ListHeader(R.string.unknown)

		ListSortOrder.OLDEST,
		ListSortOrder.NEWEST -> calculateTimeAgo(createdAt)?.let {
			ListHeader(it)
		} ?: ListHeader(R.string.unknown)

		ListSortOrder.UNREAD,
		ListSortOrder.PROGRESS -> ListHeader(
			when {
				ReadingProgress.isCompleted(percent) -> R.string.status_completed
				percent in 0f..0.01f -> R.string.status_planned
				percent in 0f..1f -> R.string.status_reading
				else -> R.string.unknown
			},
		)

		ListSortOrder.ALPHABETIC,
		ListSortOrder.ALPHABETIC_REVERSE,
		ListSortOrder.RELEVANCE,
		ListSortOrder.NEW_CHAPTERS,
		ListSortOrder.UPDATED,
		ListSortOrder.RATING -> null
	}

	private fun getEmptyState(hasFilters: Boolean) = if (hasFilters) {
		EmptyState(
			icon = R.drawable.ic_empty_history,
			textPrimary = R.string.nothing_found,
			textSecondary = R.string.text_empty_holder_secondary_filtered,
			actionStringRes = R.string.reset_filter,
		)
	} else {
		EmptyState(
			icon = R.drawable.ic_empty_history,
			textPrimary = R.string.text_history_holder_primary,
			textSecondary = R.string.text_history_holder_secondary,
			actionStringRes = 0,
		)
	}
}
