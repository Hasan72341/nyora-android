package com.nyora.hasan72341.stats.ui

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.take
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.model.FavouriteCategory
import com.nyora.hasan72341.core.ui.BaseViewModel
import com.nyora.hasan72341.core.ui.util.ReversibleAction
import com.nyora.hasan72341.core.util.ext.MutableEventFlow
import com.nyora.hasan72341.core.util.ext.call
import com.nyora.hasan72341.favourites.domain.FavouritesRepository
import com.nyora.hasan72341.stats.data.StatsRepository
import com.nyora.hasan72341.stats.domain.StatsPeriod
import com.nyora.hasan72341.stats.domain.StatsRecord
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
	private val repository: StatsRepository,
	favouritesRepository: FavouritesRepository,
) : BaseViewModel() {

	val period = MutableStateFlow(StatsPeriod.WEEK)
	val onActionDone = MutableEventFlow<ReversibleAction>()
	val selectedCategories = MutableStateFlow<Set<Long>>(emptySet())
	val favoriteCategories = favouritesRepository.observeCategories()
		.take(1)

	val readingStats = MutableStateFlow<List<StatsRecord>>(emptyList())

	init {
		launchJob(Dispatchers.IO) {
			combine<StatsPeriod, Set<Long>, Pair<StatsPeriod, Set<Long>>>(
				period,
				selectedCategories,
				::Pair,
			).collectLatest { p ->
				readingStats.value = withLoading {
					repository.getReadingStats(p.first, p.second)
				}
			}
		}
	}

	fun setCategoryChecked(category: FavouriteCategory, checked: Boolean) {
		val snapshot = selectedCategories.value.toMutableSet()
		if (checked) {
			snapshot.add(category.id)
		} else {
			snapshot.remove(category.id)
		}
		selectedCategories.value = snapshot
	}

	fun clearStats() {
		launchLoadingJob(Dispatchers.IO) {
			clearStatsStub()
			readingStats.value = emptyList()
			onActionDone.call(ReversibleAction(R.string.stats_cleared, null))
		}
	}

	// Stub for removed stats clearing API; no-op.
	private fun clearStatsStub() = Unit
}
