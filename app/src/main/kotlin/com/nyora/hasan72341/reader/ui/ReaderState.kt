package com.nyora.hasan72341.reader.ui

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.nyora.hasan72341.core.model.MangaHistory
import com.nyora.hasan72341.core.model.toChronologicalChapterOrder
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.reader.domain.readerChapters

@Parcelize
data class ReaderState(
	val chapterId: String,
	val page: Int,
	val scroll: Int,
) : Parcelable {

	constructor(history: MangaHistory) : this(
		chapterId = history.chapterId,
		page = history.page,
		scroll = history.scroll,
	)

	constructor(manga: Manga, branch: String?) : this(
		chapterId = manga.readerChapters(branch).firstOrNull()?.id
			?: manga.fallbackReaderChapters().firstOrNull()?.id
			?: error("Cannot find first chapter"),
		page = 0,
		scroll = 0,
	)
}

private fun Manga.fallbackReaderChapters() = chapters.toChronologicalChapterOrder()
