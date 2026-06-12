package com.nyora.hasan72341.list.ui.adapter

import android.view.LayoutInflater
import androidx.core.view.isVisible
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.model.getTitle
import com.nyora.hasan72341.databinding.ItemHistoryStatsBinding
import com.nyora.hasan72341.databinding.ItemHistoryStatsSourceBinding
import com.nyora.hasan72341.details.data.ReadingTime
import com.nyora.hasan72341.history.domain.model.HistoryStats
import com.nyora.hasan72341.list.ui.model.ListModel
import java.util.concurrent.TimeUnit

private const val STREAK_TARGET_DAYS = 30

/**
 * Renders the History dashboard card: day-streak ring, a bento of counters and a
 * Top Sources leaderboard. Tapping the card opens the full statistics screen.
 */
fun historyStatsAD(
	onCardClick: () -> Unit,
) = adapterDelegateViewBinding<HistoryStats, ListModel, ItemHistoryStatsBinding>(
	{ layoutInflater, parent -> ItemHistoryStatsBinding.inflate(layoutInflater, parent, false) },
) {

	binding.cardStats.setOnClickListener { onCardClick() }

	bind {
		binding.textTitles.text = item.titles.toString()
		binding.textCompleted.text = item.completed.toString()
		binding.textFavourites.text = item.favourites.toString()

		// Day-streak ring.
		binding.textStreak.text = item.streakDays.toString()
		val streakPercent = item.streakDays.coerceAtMost(STREAK_TARGET_DAYS) * 100 / STREAK_TARGET_DAYS
		binding.progressStreak.setProgressCompat(streakPercent.coerceIn(0, 100), true)

		// Reading-time tile is only meaningful while stat collection is enabled.
		binding.tileReadTime.isVisible = item.statsCollectionEnabled
		if (item.statsCollectionEnabled) {
			val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(item.readingTimeMillis).toInt()
			val readingTime = ReadingTime(
				minutes = totalMinutes % 60,
				hours = totalMinutes / 60,
				isContinue = false,
			)
			binding.textReadTime.text = readingTime.formatShort(context.resources)
				?: context.getString(R.string.minutes_short, 0)
		}

		// Top Sources leaderboard.
		val container = binding.containerSources
		container.removeAllViews()
		val hasSources = item.topSources.isNotEmpty()
		binding.titleTopSources.isVisible = hasSources
		container.isVisible = hasSources
		if (hasSources) {
			val inflater = LayoutInflater.from(context)
			val maxCount = item.topSources.maxOf { it.count }.coerceAtLeast(1)
			for (source in item.topSources) {
				val row = ItemHistoryStatsSourceBinding.inflate(inflater, container, false)
				row.textSourceName.text = source.source.getTitle(context)
				row.textSourceCount.text = source.count.toString()
				row.progressSource.setProgressCompat(
					(source.count * 100 / maxCount).coerceIn(0, 100),
					true,
				)
				container.addView(row.root)
			}
		}
	}
}
