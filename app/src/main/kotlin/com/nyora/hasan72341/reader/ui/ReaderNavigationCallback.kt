package com.nyora.hasan72341.reader.ui

import com.nyora.hasan72341.reader.domain.toChapterKey
import com.nyora.hasan72341.bookmarks.domain.Bookmark
import com.nyora.hasan72341.mihon.parsers.model.MangaChapter
import com.nyora.hasan72341.reader.ui.pager.ReaderPage

interface ReaderNavigationCallback {

	fun onPageSelected(page: ReaderPage): Boolean

	fun onChapterSelected(chapter: MangaChapter): Boolean

	fun onBookmarkSelected(bookmark: Bookmark): Boolean = onPageSelected(
		ReaderPage(bookmark.toMangaPage(), bookmark.page, bookmark.chapterId.toChapterKey()),
	)
}
