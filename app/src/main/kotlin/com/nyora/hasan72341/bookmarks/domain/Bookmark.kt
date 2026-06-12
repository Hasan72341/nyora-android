package com.nyora.hasan72341.bookmarks.domain

import com.nyora.hasan72341.core.util.MimeTypes
import com.nyora.hasan72341.core.util.ext.isImage
import com.nyora.hasan72341.list.ui.model.ListModel
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.mihon.parsers.model.MangaPage
import java.time.Instant

data class Bookmark(
	val manga: Manga,
	val pageId: String,
	val chapterId: String,
	val page: Int,
	val scroll: Int,
	val imageUrl: String,
	val createdAt: Instant,
	val percent: Float,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is Bookmark &&
			manga.id == other.manga.id &&
			chapterId == other.chapterId &&
			page == other.page
	}

	fun toMangaPage() = MangaPage(
		id = pageId,
		url = imageUrl,
		index = page,
		headers = emptyMap()
	)
}
