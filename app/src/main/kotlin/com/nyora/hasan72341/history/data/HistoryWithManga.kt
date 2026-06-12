package com.nyora.hasan72341.history.data

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.nyora.hasan72341.core.db.entity.MangaEntity

class HistoryWithManga(
	@Embedded val history: HistoryEntity,
	@Relation(
		parentColumn = "manga_id",
		entityColumn = "manga_id"
	)
	val manga: MangaEntity
)

class SourceWithCount(
	val source: String,
	val count: Int,
)