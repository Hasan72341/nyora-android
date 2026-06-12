package com.nyora.hasan72341.suggestions.data

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.nyora.hasan72341.core.db.entity.MangaEntity

data class SuggestionWithManga(
	@Embedded val suggestion: SuggestionEntity,
	@Relation(
		parentColumn = "manga_id",
		entityColumn = "manga_id"
	)
	val manga: MangaEntity
)