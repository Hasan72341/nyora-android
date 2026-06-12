package com.nyora.hasan72341.details.domain

import com.nyora.hasan72341.core.model.MangaHistory
import com.nyora.hasan72341.core.prefs.AppSettings
import com.nyora.hasan72341.details.data.MangaDetails
import com.nyora.hasan72341.details.data.ReadingTime
import com.nyora.hasan72341.stats.data.StatsRepository
import javax.inject.Inject
import kotlin.math.roundToInt

class ReadingTimeUseCase @Inject constructor(
	private val settings: AppSettings,
	private val statsRepository: StatsRepository,
) {

	suspend operator fun invoke(manga: MangaDetails?, branch: String?, history: MangaHistory?): ReadingTime? {
		if (!settings.isReadingTimeEstimationEnabled) {
			return null
		}
		val chapters = manga?.chapters?.get(branch)
		if (chapters.isNullOrEmpty()) {
			return null
		}
		val isOnHistoryBranch = history != null && chapters.any { it.id == history.chapterId.toString() }
		// Impossible task, I guess. Good luck on this.
		var averageTimeSec: Int = 20 /* pages */ * getSecondsPerPage(manga.id) * chapters.size
		if (isOnHistoryBranch) {
			averageTimeSec = (averageTimeSec * (1f - history.percent)).roundToInt()
		}
		if (averageTimeSec < 60) {
			return null
		}
		return ReadingTime(
			minutes = (averageTimeSec / 60) % 60,
			hours = averageTimeSec / 3600,
			isContinue = isOnHistoryBranch,
		)
	}

	private suspend fun getSecondsPerPage(mangaId: Long): Int {
		// getTimePerPage stats API was removed; no per-manga timing available -> use default
		var time = 0
		if (time == 0) {
			time = 10 // default
		}
		return time
	}
}
