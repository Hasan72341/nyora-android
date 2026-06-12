package com.nyora.hasan72341.local.ui

import android.content.SharedPreferences
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.model.toChipModel
import com.nyora.hasan72341.core.nav.AppRouter
import com.nyora.hasan72341.core.parser.MangaDataRepository
import com.nyora.hasan72341.core.parser.MangaRepository
import com.nyora.hasan72341.core.prefs.AppSettings
import com.nyora.hasan72341.core.prefs.ListMode
import com.nyora.hasan72341.core.ui.widgets.ChipsView
import com.nyora.hasan72341.core.util.ext.MutableEventFlow
import com.nyora.hasan72341.core.util.ext.call
import com.nyora.hasan72341.core.util.ext.toFileOrNull
import com.nyora.hasan72341.core.util.ext.toUriOrNull
import com.nyora.hasan72341.explore.data.MangaSourcesRepository
import com.nyora.hasan72341.explore.domain.ExploreRepository
import com.nyora.hasan72341.filter.ui.FilterCoordinator
import com.nyora.hasan72341.list.domain.ListFilterOption
import com.nyora.hasan72341.list.domain.MangaListMapper
import com.nyora.hasan72341.list.domain.QuickFilterListener
import com.nyora.hasan72341.list.ui.model.EmptyState
import com.nyora.hasan72341.list.ui.model.ListModel
import com.nyora.hasan72341.list.ui.model.MangaListModel
import com.nyora.hasan72341.list.ui.model.QuickFilter
import com.nyora.hasan72341.list.ui.model.TipModel
import com.nyora.hasan72341.local.data.LocalStorageChanges
import com.nyora.hasan72341.local.data.LocalStorageManager
import com.nyora.hasan72341.local.domain.DeleteLocalMangaUseCase
import com.nyora.hasan72341.local.domain.model.LocalManga
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.mihon.parsers.model.MangaTag
import com.nyora.hasan72341.remotelist.ui.RemoteListViewModel
import javax.inject.Inject
import org.koitharu.kotatsu.parsers.model.MangaTag as LibMangaTag

private fun LibMangaTag.toNyoraMangaTag(): MangaTag = MangaTag(key = key, title = title)

@HiltViewModel
class LocalListViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	mangaRepositoryFactory: MangaRepository.Factory,
	filterCoordinator: FilterCoordinator,
	private val settings: AppSettings,
	mangaListMapper: MangaListMapper,
	private val deleteLocalMangaUseCase: DeleteLocalMangaUseCase,
	exploreRepository: ExploreRepository,
	@param:LocalStorageChanges private val localStorageChanges: SharedFlow<LocalManga?>,
	private val localStorageManager: LocalStorageManager,
	sourcesRepository: MangaSourcesRepository,
	mangaDataRepository: MangaDataRepository,
) : RemoteListViewModel(
	savedStateHandle = savedStateHandle,
	mangaRepositoryFactory = mangaRepositoryFactory,
	filterCoordinator = filterCoordinator,
	settings = settings,
	mangaListMapper = mangaListMapper,
	exploreRepository = exploreRepository,
	sourcesRepository = sourcesRepository,
	mangaDataRepository = mangaDataRepository,
	localStorageChanges = localStorageChanges,
), SharedPreferences.OnSharedPreferenceChangeListener, QuickFilterListener {

	val onMangaRemoved = MutableEventFlow<Unit>()
	private val showInlineFilter: Boolean = savedStateHandle[AppRouter.KEY_IS_BOTTOMTAB] ?: false

	init {
		launchJob(Dispatchers.IO) {
			localStorageChanges
				.collect {
					loadList(filterCoordinator.snapshot(), append = false).join()
				}
		}
		settings.subscribe(this)
	}

	override suspend fun onBuildList(list: MutableList<ListModel>) {
		super.onBuildList(list)
		if (showInlineFilter) {
			createFilterHeader(maxCount = 16)?.let {
				list.add(0, it)
			}
		}
		if (!localStorageManager.hasExternalStoragePermission(isReadOnly = true)) {
			for (item in list) {
				if (item !is MangaListModel) {
					continue
				}
				val file = item.manga.url.toUriOrNull()?.toFileOrNull() ?: continue
				if (localStorageManager.isOnExternalStorage(file)) {
					val tip = TipModel(
						key = "permission",
						title = R.string.external_storage,
						text = R.string.missing_storage_permission,
						icon = R.drawable.ic_storage,
						primaryButtonText = R.string.fix,
						secondaryButtonText = R.string.settings,
					)
					list.add(0, tip)
					return
				}
			}
		}
	}

	override fun setFilterOption(option: ListFilterOption, isApplied: Boolean) {
		if (option is ListFilterOption.Tag) {
			filterCoordinator.toggleTag(option.tag, isApplied)
		}
	}

	override fun toggleFilterOption(option: ListFilterOption) {
		if (option is ListFilterOption.Tag) {
			val tag = option.tag
			val isSelected = filterCoordinator.snapshot().listFilter.tags.any { it.key == tag.key }
			filterCoordinator.toggleTag(option.tag, !isSelected)
		}
	}

	override fun clearFilter() = filterCoordinator.reset()

	override fun onCleared() {
		settings.unsubscribe(this)
		super.onCleared()
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
		if (key == AppSettings.KEY_LOCAL_MANGA_DIRS) {
			onRefresh()
		}
	}

	fun delete(ids: Set<String>) {
		launchLoadingJob(Dispatchers.IO) {
			deleteLocalMangaUseCase(ids)
			onMangaRemoved.call(Unit)
		}
	}

	override suspend fun mapMangaList(
		destination: MutableCollection<in ListModel>,
		manga: Collection<Manga>,
		mode: ListMode
	) = mangaListMapper.toListModelList(destination, manga, mode, MangaListMapper.NO_SAVED)

	override fun createEmptyState(canResetFilter: Boolean): EmptyState = if (canResetFilter) {
		super.createEmptyState(true)
	} else {
		EmptyState(
			icon = R.drawable.ic_empty_local,
			textPrimary = R.string.text_local_holder_primary,
			textSecondary = R.string.text_local_holder_secondary,
			actionStringRes = R.string._import,
		)
	}

	private suspend fun createFilterHeader(maxCount: Int): QuickFilter? {
		val appliedTags = filterCoordinator.snapshot().listFilter.tags
		val availableTags = repository.getFilterOptions().availableTags
		if (appliedTags.isEmpty() && availableTags.size < 3) {
			return null
		}
		val result = ArrayList<ChipsView.ChipModel>(minOf(availableTags.size, maxCount))
		appliedTags.mapTo(result) { tag ->
			ListFilterOption.Tag(tag.toNyoraMangaTag()).toChipModel(isChecked = true)
		}
		for (tag in availableTags) {
			if (result.size >= maxCount) {
				break
			}
			if (tag in appliedTags) {
				continue
			}
			result.add(ListFilterOption.Tag(tag.toNyoraMangaTag()).toChipModel(isChecked = false))
		}
		return QuickFilter(result)
	}
}
