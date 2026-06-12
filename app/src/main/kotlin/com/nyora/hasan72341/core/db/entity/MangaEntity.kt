package com.nyora.hasan72341.core.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nyora.hasan72341.core.db.TABLE_MANGA

@Entity(tableName = TABLE_MANGA)
data class MangaEntity(
	@PrimaryKey(autoGenerate = false)
	@ColumnInfo(name = "manga_id") val id: String,
	@ColumnInfo(name = "title") val title: String,
	@ColumnInfo(name = "alt_title") val altTitles: String?,
	@ColumnInfo(name = "url") val url: String,
	@ColumnInfo(name = "public_url") val publicUrl: String,
	@ColumnInfo(name = "rating") val rating: Float, // normalized value [0..1] or -1
	@ColumnInfo(name = "nsfw") val isNsfw: Boolean,
	@ColumnInfo(name = "content_rating") val contentRating: String?,
	@ColumnInfo(name = "cover_url") val coverUrl: String,
	@ColumnInfo(name = "large_cover_url") val largeCoverUrl: String?,
	@ColumnInfo(name = "state") val state: String?,
	@ColumnInfo(name = "author") val authors: String?,
	@ColumnInfo(name = "source") val source: String,
	@ColumnInfo(name = "description") val description: String = "",
	@ColumnInfo(name = "tags") val tags: String = "[]",
	@ColumnInfo(name = "chapters") val chapters: String = "[]",
	@ColumnInfo(name = "unread") val unread: Int = 0,
	@ColumnInfo(name = "progress") val progress: Float = 0f,
)
