package com.nyora.hasan72341.reader.domain

import com.nyora.hasan72341.core.model.toChronologicalChapterOrder
import com.nyora.hasan72341.details.data.MangaDetails
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.mihon.parsers.model.MangaChapter

fun MangaDetails.readerChapters(branch: String?): List<MangaChapter> =
	chapters[branch].orEmpty().toChronologicalChapterOrder()

fun MangaDetails.readerChaptersFor(chapterId: String): List<MangaChapter> {
	val chapter = allChapters.find { it.id == chapterId } ?: return allChapters.toChronologicalChapterOrder()
	return readerChapters(chapter.branch)
}

fun Manga.readerChapters(branch: String?): List<MangaChapter> =
	chapters.filter { it.branch == branch }.toChronologicalChapterOrder()

