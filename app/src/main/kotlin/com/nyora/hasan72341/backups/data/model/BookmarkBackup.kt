package com.nyora.hasan72341.backups.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.nyora.hasan72341.bookmarks.data.BookmarkEntity
import com.nyora.hasan72341.core.db.entity.MangaEntity

@Serializable
class BookmarkBackup(
	@SerialName("manga") val manga: MangaBackup,
	@SerialName("bookmarks") val bookmarks: List<Bookmark>,
) {

	@Serializable
	class Bookmark(
		@SerialName("manga_id") val mangaId: String,
		@SerialName("page_id") val pageId: String,
		@SerialName("chapter_id") val chapterId: String,
		@SerialName("page") val page: Int,
		@SerialName("scroll") val scroll: Int,
		@SerialName("image_url") val imageUrl: String,
		@SerialName("created_at") val createdAt: Long,
		@SerialName("percent") val percent: Float,
	) {

		fun toEntity() = BookmarkEntity(
			mangaId = mangaId,
			pageId = pageId,
			chapterId = chapterId,
			page = page,
			scroll = scroll,
			imageUrl = imageUrl,
			createdAt = createdAt,
			percent = percent,
			deletedAt = 0L,
		)
	}

	constructor(manga: MangaEntity, entities: List<BookmarkEntity>) : this(
		manga = MangaBackup(manga),
		bookmarks = entities.map {
			Bookmark(
				mangaId = it.mangaId,
				pageId = it.pageId,
				chapterId = it.chapterId,
				page = it.page,
				scroll = it.scroll,
				imageUrl = it.imageUrl,
				createdAt = it.createdAt,
				percent = it.percent,
			)
		},
	)
}
