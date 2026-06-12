package com.nyora.hasan72341.stats.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import com.nyora.hasan72341.history.data.HistoryEntity

@Entity(
	tableName = "stats",
	primaryKeys = ["manga_id", "started_at"],
	foreignKeys = [
		ForeignKey(
			entity = HistoryEntity::class,
			parentColumns = ["manga_id"],
			childColumns = ["manga_id"],
			onDelete = ForeignKey.CASCADE,
		),
	],
)
data class StatsEntity(
	@ColumnInfo(name = "manga_id") val mangaId: String,
	@ColumnInfo(name = "started_at") val startedAt: Long,
	@ColumnInfo(name = "duration") val duration: Long,
	@ColumnInfo(name = "pages") val pages: Int,
)
