package com.nyora.hasan72341.list.ui

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import com.nyora.hasan72341.core.model.isNsfw
import com.nyora.hasan72341.core.parser.MangaDataRepository
import com.nyora.hasan72341.core.prefs.AppSettings
import com.nyora.hasan72341.core.prefs.ListMode
import com.nyora.hasan72341.core.prefs.observeAsFlow
import com.nyora.hasan72341.core.prefs.observeAsStateFlow
import com.nyora.hasan72341.core.ui.BaseViewModel
import com.nyora.hasan72341.core.ui.util.ReversibleAction
import com.nyora.hasan72341.core.util.ext.MutableEventFlow
import com.nyora.hasan72341.list.domain.ListFilterOption
import com.nyora.hasan72341.list.ui.model.ListModel
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.local.data.LocalStorageChanges
import com.nyora.hasan72341.local.domain.model.LocalManga

abstract class MangaListViewModel(
	private val settings: AppSettings,
	private val mangaDataRepository: MangaDataRepository,
	@param:LocalStorageChanges private val localStorageChanges: SharedFlow<LocalManga?>,
) : BaseViewModel() {

	abstract val content: StateFlow<List<ListModel>>
	open val listMode = settings.observeAsFlow(AppSettings.KEY_LIST_MODE) { listMode }
		.stateIn(viewModelScope + Dispatchers.IO, SharingStarted.Eagerly, settings.listMode)
	val onActionDone = MutableEventFlow<ReversibleAction>()
	val gridScale = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.IO,
		key = AppSettings.KEY_GRID_SIZE,
		valueProducer = { gridSize / 100f },
	)

	val isIncognitoModeEnabled: Boolean
		get() = settings.isIncognitoModeEnabled

	abstract fun onRefresh()

	abstract fun onRetry()

	protected fun List<Manga>.skipNsfwIfNeeded() = if (settings.isNsfwContentDisabled) {
		filterNot { it.isNsfw() }
	} else {
		this
	}

	protected fun Flow<Set<ListFilterOption>>.combineWithSettings(): Flow<Set<ListFilterOption>> = combine(
		settings.observeAsFlow(AppSettings.KEY_DISABLE_NSFW) { isNsfwContentDisabled },
	) { filters, skipNsfw ->
		if (skipNsfw) {
			filters + ListFilterOption.SFW
		} else {
			filters
		}
	}

	protected fun observeListModeWithTriggers(): Flow<ListMode> = combine(
		listMode,
		merge(
			mangaDataRepository.observeOverridesTrigger(emitInitialState = true),
			mangaDataRepository.observeFavoritesTrigger(emitInitialState = true),
			localStorageChanges.onStart { emit(null) },
		),
		settings.observeChanges().filter { key ->
			key == AppSettings.KEY_PROGRESS_INDICATORS
				|| key == AppSettings.KEY_TRACKER_ENABLED
				|| key == AppSettings.KEY_QUICK_FILTER
				|| key == AppSettings.KEY_MANGA_LIST_BADGES
		}.onStart { emit("") },
	) { mode, _, _ ->
		mode
	}
}
