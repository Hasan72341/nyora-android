package com.nyora.hasan72341.tracker.domain

import com.nyora.hasan72341.core.prefs.AppSettings
import com.nyora.hasan72341.favourites.domain.FavouritesRepository
import com.nyora.hasan72341.list.domain.ListFilterOption
import com.nyora.hasan72341.list.domain.MangaListQuickFilter
import javax.inject.Inject

class UpdatesListQuickFilter @Inject constructor(
	private val favouritesRepository: FavouritesRepository,
	settings: AppSettings,
) : MangaListQuickFilter(settings) {

	override suspend fun getAvailableFilterOptions(): List<ListFilterOption> =
		favouritesRepository.getMostUpdatedCategories(
			limit = 4,
		).map {
			ListFilterOption.Favorite(it)
		}
}
