package com.nyora.hasan72341.settings.tracker.categories

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import com.nyora.hasan72341.core.model.FavouriteCategory
import com.nyora.hasan72341.core.ui.BaseViewModel
import com.nyora.hasan72341.favourites.domain.FavouritesRepository
import javax.inject.Inject

@HiltViewModel
class TrackerCategoriesConfigViewModel @Inject constructor(
	private val favouritesRepository: FavouritesRepository,
) : BaseViewModel() {

	val content = favouritesRepository.observeCategories()
		.stateIn(viewModelScope + Dispatchers.IO, SharingStarted.Eagerly, emptyList())

	private var updateJob: Job? = null

	fun toggleItem(category: FavouriteCategory) {
		val prevJob = updateJob
		updateJob = launchJob(Dispatchers.IO) {
			prevJob?.join()
			favouritesRepository.updateCategoryTracking(category.id, !category.isTrackingEnabled)
		}
	}
}
