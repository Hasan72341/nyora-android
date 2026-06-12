package com.nyora.hasan72341.bookmarks.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import com.nyora.hasan72341.core.db.entity.MangaEntity

@Entity(
	tableName = "bookmarks",
	primaryKeys = ["manga_id", "page_id"],
	foreignKeys = [
		ForeignKey(
			entity = MangaEntity::class,
			parentColumns = ["manga_id"],
			childColumns = ["manga_id"],
			onDelete = ForeignKey.CASCADE
		),
	]
)
data class BookmarkEntity(
	@ColumnInfo(name = "manga_id", index = true) val mangaId: String,
	@ColumnInfo(name = "page_id", index = true) val pageId: String,
	@ColumnInfo(name = "chapter_id") val chapterId: String,
	@ColumnInfo(name = "page") val page: Int,
	@ColumnInfo(name = "scroll") val scroll: Int,
	@ColumnInfo(name = "image") val imageUrl: String,
	@ColumnInfo(name = "created_at") val createdAt: Long,
	@ColumnInfo(name = "percent") val percent: Float,
	@ColumnInfo(name = "deleted_at") val deletedAt: Long,
)
