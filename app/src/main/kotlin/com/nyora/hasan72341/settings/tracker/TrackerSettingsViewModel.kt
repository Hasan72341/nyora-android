package com.nyora.hasan72341.settings.tracker

import androidx.room.InvalidationTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import okio.Closeable
import com.nyora.hasan72341.core.db.MangaDatabase
import com.nyora.hasan72341.core.db.TABLE_FAVOURITE_CATEGORIES
import com.nyora.hasan72341.core.db.removeObserverAsync
import com.nyora.hasan72341.core.ui.BaseViewModel
import com.nyora.hasan72341.tracker.domain.TrackingRepository
import javax.inject.Inject

@HiltViewModel
class TrackerSettingsViewModel @Inject constructor(
	private val repository: TrackingRepository,
	private val database: MangaDatabase,
) : BaseViewModel() {

	val categoriesCount = MutableStateFlow<IntArray?>(null)

	init {
		updateCategoriesCount()
		val databaseObserver = DatabaseObserver(this)
		addCloseable(databaseObserver)
		launchJob(Dispatchers.IO) {
			database.invalidationTracker.addObserver(databaseObserver)
		}
	}

	private fun updateCategoriesCount() {
		launchJob(Dispatchers.IO) {
			categoriesCount.value = repository.getCategoriesCount()
		}
	}

	private class DatabaseObserver(private var vm: TrackerSettingsViewModel?) :
		InvalidationTracker.Observer(arrayOf(TABLE_FAVOURITE_CATEGORIES)),
		Closeable {

		override fun onInvalidated(tables: Set<String>) {
			vm?.updateCategoriesCount()
		}

		override fun close() {
			(vm ?: return).database.invalidationTracker.removeObserverAsync(this)
			vm = null
		}
	}
}
