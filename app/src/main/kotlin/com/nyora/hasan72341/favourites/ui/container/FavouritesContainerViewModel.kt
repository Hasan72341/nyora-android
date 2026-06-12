package com.nyora.hasan72341.favourites.ui.container

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.model.FavouriteCategory
import com.nyora.hasan72341.core.prefs.AppSettings
import com.nyora.hasan72341.core.prefs.observeAsFlow
import com.nyora.hasan72341.core.ui.BaseViewModel
import com.nyora.hasan72341.core.ui.util.ReversibleAction
import com.nyora.hasan72341.core.ui.util.ReversibleHandle
import com.nyora.hasan72341.core.util.ext.MutableEventFlow
import com.nyora.hasan72341.core.util.ext.call
import com.nyora.hasan72341.favourites.domain.FavouritesRepository
import com.nyora.hasan72341.favourites.ui.list.FavouritesListFragment.Companion.NO_ID
import javax.inject.Inject

@HiltViewModel
class FavouritesContainerViewModel @Inject constructor(
	private val settings: AppSettings,
	private val favouritesRepository: FavouritesRepository,
) : BaseViewModel() {

	val onActionDone = MutableEventFlow<ReversibleAction>()

	private val categoriesStateFlow = favouritesRepository.observeCategoriesForLibrary()
		.withErrorHandling()
		.stateIn(viewModelScope + Dispatchers.IO, SharingStarted.Eagerly, null)

	val categories = combine(
		categoriesStateFlow.filterNotNull(),
		observeAllFavouritesVisibility(),
	) { list, showAll ->
		list.toUi(showAll)
	}.stateIn(viewModelScope + Dispatchers.IO, SharingStarted.Eagerly, emptyList())

	val isEmpty = categoriesStateFlow.map {
		it?.isEmpty() == true
	}.stateIn(viewModelScope + Dispatchers.IO, SharingStarted.Eagerly, false)

	private fun List<FavouriteCategory>.toUi(showAll: Boolean): List<FavouriteTabModel> {
		if (isEmpty() && !showAll) {
			return listOf(FavouriteTabModel(ID_DOWNLOADS, null))
		}
		val result = ArrayList<FavouriteTabModel>(size + (if (showAll) 1 else 0) + 1)
		if (showAll) {
			result.add(FavouriteTabModel(NO_ID, null))
		}
		mapTo(result) { FavouriteTabModel(it.id, it.title) }
		result.add(FavouriteTabModel(ID_DOWNLOADS, null))
		return result
	}

	fun hide(categoryId: Long) {
		launchJob(Dispatchers.IO) {
			if (categoryId == NO_ID) {
				settings.isAllFavouritesVisible = false
			} else if (categoryId != ID_DOWNLOADS) {
				favouritesRepository.updateCategory(categoryId, isVisibleInLibrary = false)
				val reverse = ReversibleHandle {
					favouritesRepository.updateCategory(categoryId, isVisibleInLibrary = true)
				}
				onActionDone.call(ReversibleAction(R.string.category_hidden_done, reverse))
			}
		}
	}

	fun deleteCategory(categoryId: Long) {
		launchJob(Dispatchers.IO) {
			if (categoryId != ID_DOWNLOADS && categoryId != NO_ID) {
				favouritesRepository.removeCategories(setOf(categoryId))
			}
		}
	}

	private fun observeAllFavouritesVisibility() = settings.observeAsFlow(
		key = AppSettings.KEY_ALL_FAVOURITES_VISIBLE,
		valueProducer = { isAllFavouritesVisible },
	)

	companion object {

		const val ID_DOWNLOADS = -1L
	}
}
