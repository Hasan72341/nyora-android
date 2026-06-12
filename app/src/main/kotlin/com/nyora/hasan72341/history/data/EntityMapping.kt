package com.nyora.hasan72341.history.data

import com.nyora.hasan72341.core.model.MangaHistory
import java.time.Instant

fun HistoryEntity.toMangaHistory() = MangaHistory(
	createdAt = Instant.ofEpochMilli(createdAt),
	updatedAt = Instant.ofEpochMilli(updatedAt),
	chapterId = chapterId.toLongOrNull() ?: 0L,
	page = page,
	scroll = scroll.toInt(),
	percent = percent,
	chaptersCount = chaptersCount,
)
