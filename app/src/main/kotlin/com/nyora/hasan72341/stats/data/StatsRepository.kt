package com.nyora.hasan72341.stats.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import com.nyora.hasan72341.core.db.MangaDatabase
import com.nyora.hasan72341.core.db.entity.toManga
import com.nyora.hasan72341.core.prefs.AppSettings
import com.nyora.hasan72341.core.prefs.observeAsFlow
import com.nyora.hasan72341.stats.domain.StatsPeriod
import com.nyora.hasan72341.stats.domain.StatsRecord
import java.util.NavigableMap
import java.util.TreeMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class StatsRepository @Inject constructor(
	private val settings: AppSettings,
	private val db: MangaDatabase,
) {

	suspend fun getReadingStats(period: StatsPeriod, categories: Set<Long>): List<StatsRecord> {
		val fromDate = if (period == StatsPeriod.ALL) {
			0L
		} else {
			System.currentTimeMillis() - TimeUnit.DAYS.toMillis(period.days.toLong())
		}
		val stats = db.getStatsDao().getDurationStats(fromDate, null, categories)
		val result = ArrayList<StatsRecord>(stats.size)
		var other = StatsRecord(null, 0)
		val total = stats.values.sum()
		for ((mangaEntity, duration) in stats) {
			val manga = mangaEntity.toManga()
			val percent = duration.toDouble() / total
			if (percent < 0.05) {
				other = other.copy(duration = other.duration + duration)
			} else {
				result += StatsRecord(
					manga = manga,
					duration = duration,
				)
			}
		}
		return result
	}

		suspend fun getTimePerPage(mangaId: String): Long = db.withTransaction {
			val dao = db.getStatsDao()
			val pages = dao.getReadPagesCount(mangaId)
			val time = if (pages >= 10) {
				dao.getAverageTimePerPage(mangaId)
			} else {
				dao.getAverageTimePerPage()
			}
			time
		}

		suspend fun getTotalPagesRead(mangaId: String): Int {
			return db.getStatsDao().getReadPagesCount(mangaId)
		}

		suspend fun getMangaTimeline(mangaId: String): NavigableMap<Long, Int> {
			val entities = db.getStatsDao().findAll(mangaId)
			val map = TreeMap<Long, Int>()
			for (e in entities) {
				map[e.startedAt] = e.pages
			}
			return map
		}

		suspend fun clearStats() {
			db.getStatsDao().clear()
		}

		fun observeHasStats(mangaId: String): Flow<Boolean> = settings.observeAsFlow(AppSettings.KEY_STATS_ENABLED) {
			isStatsEnabled
		}.flatMapLatest { isEnabled ->
			if (isEnabled) {
				db.getStatsDao().observeRowCount(mangaId).map { it > 0 }
			} else {
				flowOf(false)
			}
		}.distinctUntilChanged()
}
