package com.nyora.hasan72341.history.domain

import com.nyora.hasan72341.core.db.MangaDatabase
import com.nyora.hasan72341.core.model.MangaSource
import com.nyora.hasan72341.core.prefs.AppSettings
import com.nyora.hasan72341.core.prefs.observeAsFlow
import com.nyora.hasan72341.history.domain.model.HistoryStats
import com.nyora.hasan72341.history.domain.model.HistoryStatsSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/**
 * Aggregates the reading stats shown in the History dashboard card.
 *
 * Emits `null` while the history is empty so the dashboard is hidden. Reading time always
 * comes from the stats table; the day-streak falls back to history timestamps when
 * stat-collection is disabled so the ring still reflects reading days.
 */
class HistoryStatsUseCase @Inject constructor(
	private val db: MangaDatabase,
	private val settings: AppSettings,
) {

	operator fun invoke(): Flow<HistoryStats?> {
		val historyDao = db.getHistoryDao()
		return combine(
			historyDao.observeCount(),
			historyDao.observeCompletedCount(),
			db.getFavouritesDao().observeMangaCount(),
			historyDao.observeTopSources(TOP_SOURCES_LIMIT),
			historyDao.observeAllTimestamps(),
		) { titles, completed, favourites, topSourcesRaw, timestamps ->
			val topSources = topSourcesRaw.map { 
				HistoryStatsSource(MangaSource(it.source), it.count)
			}
			val readingTime = 0L // Still hardcoded as we don't have reading time tracking yet
			HistoryStats(
				titles = titles,
				completed = completed,
				favourites = favourites,
				streakDays = longestStreak(timestamps.toLongArray()),
				readingTimeMillis = readingTime,
				statsCollectionEnabled = true,
				topSources = topSources,
			)
		}.distinctUntilChanged().flowOn(Dispatchers.IO)
	}

	/**
	 * Longest run of consecutive calendar days (system time-zone) that contain any activity.
	 */
	private fun longestStreak(timestamps: LongArray): Int {
		if (timestamps.isEmpty()) return 0
		val zone = ZoneId.systemDefault()
		val days = timestamps
			.asSequence()
			.filter { it > 0L }
			.map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate().toEpochDay() }
			.toSortedSet()
		if (days.isEmpty()) return 0
		var longest = 1
		var current = 1
		var prev = days.first()
		for (day in days.drop(1)) {
			current = if (day == prev + 1L) current + 1 else 1
			if (current > longest) longest = current
			prev = day
		}
		return longest
	}

	private companion object {

		const val TOP_SOURCES_LIMIT = 4
	}
}
