package com.nyora.hasan72341.history.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.nyora.hasan72341.core.db.TABLE_HISTORY
import com.nyora.hasan72341.core.db.entity.MangaEntity

@Entity(
	tableName = TABLE_HISTORY,
	foreignKeys = [
		ForeignKey(
			entity = MangaEntity::class,
			parentColumns = ["manga_id"],
			childColumns = ["manga_id"],
			onDelete = ForeignKey.CASCADE,
		),
	],
)
data class HistoryEntity(
	@PrimaryKey(autoGenerate = false)
	@ColumnInfo(name = "manga_id") val mangaId: String,
	@ColumnInfo(name = "created_at") val createdAt: Long,
	@ColumnInfo(name = "updated_at") val updatedAt: Long,
	@ColumnInfo(name = "chapter_id") val chapterId: String,
	@ColumnInfo(name = "page") val page: Int,
	@ColumnInfo(name = "scroll") val scroll: Float,
	@ColumnInfo(name = "percent") val percent: Float,
	@ColumnInfo(name = "deleted_at") val deletedAt: Long,
	@ColumnInfo(name = "chapters") val chaptersCount: Int,
)
