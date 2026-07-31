package com.nyora.hasan72341.list.ui.config

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import com.nyora.hasan72341.core.nav.AppRouter
import com.nyora.hasan72341.core.prefs.AppSettings
import com.nyora.hasan72341.core.prefs.ListMode
import com.nyora.hasan72341.core.ui.BaseViewModel
import com.nyora.hasan72341.core.util.ext.require
import com.nyora.hasan72341.core.util.ext.sortedByOrdinal
import com.nyora.hasan72341.favourites.domain.FavouritesRepository
import com.nyora.hasan72341.favourites.ui.list.FavouritesListFragment.Companion.NO_ID
import com.nyora.hasan72341.list.domain.ListSortOrder
import com.nyora.hasan72341.mihon.parsers.util.runCatchingCancellable
import javax.inject.Inject

@HiltViewModel
class ListConfigViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val settings: AppSettings,
	private val favouritesRepository: FavouritesRepository,
) : BaseViewModel() {

	val section = savedStateHandle.require<ListConfigSection>(AppRouter.KEY_LIST_SECTION)

	// Preloaded off the main thread so opening the sheet never does a blocking Room read.
	@Volatile
	private var cachedCategoryOrder: ListSortOrder? = null

	init {
		val fav = section as? ListConfigSection.Favorites
		if (fav != null && fav.categoryId != NO_ID) {
			launchJob {
				cachedCategoryOrder = runCatchingCancellable {
					favouritesRepository.getCategory(fav.categoryId).order
				}.getOrNull()
			}
		}
	}

	var listMode: ListMode
		get() = when (section) {
			is ListConfigSection.Favorites -> settings.favoritesListMode
			ListConfigSection.History -> settings.historyListMode
			ListConfigSection.Suggestions -> settings.suggestionsListMode
			ListConfigSection.General,
			ListConfigSection.Updated -> settings.listMode
		}
		set(value) {
			when (section) {
				is ListConfigSection.Favorites -> settings.favoritesListMode = value
				ListConfigSection.History -> settings.historyListMode = value
				ListConfigSection.Suggestions -> settings.suggestionsListMode = value
				ListConfigSection.Updated,
				ListConfigSection.General -> settings.listMode = value
			}
		}

	var gridSize: Int
		get() = settings.gridSize
		set(value) {
			settings.gridSize = value
		}

	val isGroupingSupported: Boolean
		get() = section == ListConfigSection.History || section == ListConfigSection.Updated

	val isGroupingAvailable: Boolean
		get() = when (section) {
			ListConfigSection.History -> settings.historySortOrder.isGroupingSupported()
			ListConfigSection.Updated -> true
			else -> false
		}

	var isGroupingEnabled: Boolean
		get() = when (section) {
			ListConfigSection.History -> settings.isHistoryGroupingEnabled
			ListConfigSection.Updated -> settings.isUpdatedGroupingEnabled
			else -> false
		}
		set(value) = when (section) {
			ListConfigSection.History -> settings.isHistoryGroupingEnabled = value
			ListConfigSection.Updated -> settings.isUpdatedGroupingEnabled = value
			else -> Unit
		}

	fun getSortOrders(): List<ListSortOrder>? = when (section) {
		is ListConfigSection.Favorites -> ListSortOrder.FAVORITES
		ListConfigSection.General -> null
		ListConfigSection.History -> ListSortOrder.HISTORY
		ListConfigSection.Suggestions -> ListSortOrder.SUGGESTIONS
		ListConfigSection.Updated -> null
	}?.sortedByOrdinal()

	fun getSelectedSortOrder(): ListSortOrder? = when (section) {
		is ListConfigSection.Favorites -> getCategorySortOrder(section.categoryId)
		ListConfigSection.General -> null
		ListConfigSection.Updated -> null
		ListConfigSection.History -> settings.historySortOrder
		ListConfigSection.Suggestions -> ListSortOrder.RELEVANCE
	}

	fun setSortOrder(position: Int) {
		val value = getSortOrders()?.getOrNull(position) ?: return
		when (section) {
			is ListConfigSection.Favorites -> launchJob {
				if (section.categoryId == NO_ID) {
					settings.allFavoritesSortOrder = value
				} else {
					favouritesRepository.setCategoryOrder(section.categoryId, value)
				}
			}

			ListConfigSection.General -> Unit
			ListConfigSection.History -> settings.historySortOrder = value

			ListConfigSection.Suggestions -> Unit
			ListConfigSection.Updated -> Unit
		}
	}

	private fun getCategorySortOrder(id: Long): ListSortOrder = if (id == NO_ID) {
		settings.allFavoritesSortOrder
	} else {
		// Uses the value preloaded in init(); falls back until it lands (avoids a main-thread query).
		cachedCategoryOrder ?: settings.allFavoritesSortOrder
	}
}
