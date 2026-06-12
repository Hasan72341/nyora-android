package com.nyora.hasan72341.history.domain.model

import com.nyora.hasan72341.list.ui.model.ListModel
import com.nyora.hasan72341.mihon.parsers.model.MangaSource

/**
 * A single source row of the History stats dashboard's "Top Sources" leaderboard.
 */
data class HistoryStatsSource(
	val source: MangaSource,
	val count: Int,
)

/**
 * Aggregate reading stats rendered as a dashboard card pinned to the top of the History list.
 *
 * Mirrors the Nyora desktop StatsScreen: a day-streak ring, a bento of counters
 * (titles / completed / favourites / reading time) and a Top Sources leaderboard.
 * [readingTimeMillis] and the streak are only meaningful when [statsCollectionEnabled] is true.
 */
data class HistoryStats(
	val titles: Int,
	val completed: Int,
	val favourites: Int,
	val streakDays: Int,
	val readingTimeMillis: Long,
	val statsCollectionEnabled: Boolean,
	val topSources: List<HistoryStatsSource>,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean = other is HistoryStats
}
