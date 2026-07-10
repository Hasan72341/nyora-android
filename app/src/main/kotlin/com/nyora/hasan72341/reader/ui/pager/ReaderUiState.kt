package com.nyora.hasan72341.reader.ui.pager

import android.content.res.Resources
import com.nyora.hasan72341.core.model.getLocalizedTitle
import com.nyora.hasan72341.mihon.parsers.model.MangaChapter

data class ReaderUiState(
	val mangaName: String?,
	val chapter: MangaChapter,
	val chapterIndex: Int,
	val chaptersTotal: Int,
	val currentPage: Int,
	val totalPages: Int,
	val percent: Float,
	val incognito: Boolean,
) {

	val chapterNumber: Int
		get() = if (chapterIndex >= 0) chapterIndex + 1 else 0

	fun hasNextChapter(): Boolean = chapterIndex >= 0 && chapterIndex < chaptersTotal - 1

	fun hasPreviousChapter(): Boolean = chapterIndex > 0

	fun isSliderAvailable(): Boolean = totalPages > 1 && currentPage < totalPages

	fun getChapterTitle(resources: Resources) = chapter.getLocalizedTitle(resources, chapterIndex)
}
