package com.nyora.hasan72341.stats.domain

import androidx.collection.LongSparseArray
import androidx.collection.set
import dagger.hilt.android.ViewModelLifecycle
import dagger.hilt.android.scopes.ViewModelScoped
import com.nyora.hasan72341.core.db.MangaDatabase
import com.nyora.hasan72341.core.prefs.AppSettings
import com.nyora.hasan72341.core.util.RetainedLifecycleCoroutineScope
import com.nyora.hasan72341.core.util.ext.printStackTraceDebug
import com.nyora.hasan72341.mihon.parsers.util.runCatchingCancellable
import com.nyora.hasan72341.reader.ui.ReaderState
import com.nyora.hasan72341.stats.data.StatsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@ViewModelScoped
class StatsCollector @Inject constructor(
	private val db: MangaDatabase,
	private val settings: AppSettings,
	lifecycle: ViewModelLifecycle,
) {

	private val viewModelScope = RetainedLifecycleCoroutineScope(lifecycle)
	private val stats = mutableMapOf<String, Entry>()

	@Synchronized
	fun onStateChanged(mangaId: String, state: ReaderState) {
		if (!settings.isStatsEnabled) {
			return
		}
		val now = System.currentTimeMillis()
		val entry = stats[mangaId]
		if (entry == null) {
			stats[mangaId] = Entry(
				state = state,
				stats = StatsEntity(
					mangaId = mangaId,
					startedAt = now,
					duration = 0,
					pages = 0,
				),
			)
			return
		}
		val pagesDelta = if (entry.state.page != state.page || entry.state.chapterId != state.chapterId) 1 else 0
		val newEntry = entry.copy(
			stats = StatsEntity(
				mangaId = mangaId,
				startedAt = entry.stats.startedAt,
				duration = now - entry.stats.startedAt,
				pages = entry.stats.pages + pagesDelta,
			),
		)
		stats[mangaId] = newEntry
		commit(newEntry.stats)
	}

	@Synchronized
	fun onPause(mangaId: String) {
		stats.remove(mangaId)
	}

	private fun commit(entity: StatsEntity) {
		viewModelScope.launch(Dispatchers.IO) {
			runCatchingCancellable {
				db.getStatsDao().upsert(entity)
			}.onFailure { e ->
				e.printStackTraceDebug("StatsCollector::commit")
			}
		}
	}

	private data class Entry(
		val state: ReaderState,
		val stats: StatsEntity,
	)
}
