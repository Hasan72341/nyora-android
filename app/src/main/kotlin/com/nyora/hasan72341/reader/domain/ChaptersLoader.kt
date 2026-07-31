package com.nyora.hasan72341.reader.domain

import androidx.annotation.CheckResult
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.nyora.hasan72341.core.parser.MangaRepository
import com.nyora.hasan72341.details.data.MangaDetails
import com.nyora.hasan72341.mihon.parsers.model.MangaChapter
import com.nyora.hasan72341.mihon.parsers.model.MangaPage
import com.nyora.hasan72341.reader.ui.pager.ReaderPage
import javax.inject.Inject

private const val PAGES_TRIM_THRESHOLD = 120

@ViewModelScoped
class ChaptersLoader @Inject constructor(
	private val mangaRepositoryFactory: MangaRepository.Factory,
) {

	private val chapters = LinkedHashMap<String, MangaChapter>()
	private val chapterPages = ChapterPages()
	private val mutex = Mutex()
	private var currentMangaSource: String? = null

	val size: Int
		get() = chapters.size

	suspend fun init(manga: MangaDetails) = mutex.withLock {
		chapters.clear()
		currentMangaSource = manga.toManga().source.name
		manga.allChapters.forEach {
			chapters[it.id] = it
		}
	}

	suspend fun loadPrevNextChapter(manga: MangaDetails, currentId: String, isNext: Boolean): Boolean {
		val predicate: (MangaChapter) -> Boolean = { it.id == currentId }
		val currentChapter = manga.allChapters.find(predicate) ?: return false
		val chapters = manga.readerChapters(currentChapter.branch)
		val index = chapters.indexOfFirst(predicate)
		if (index == -1) return false
		val newChapter = chapters.getOrNull(if (isNext) index + 1 else index - 1) ?: return false
		val newPages = loadChapter(newChapter.id)
		mutex.withLock {
			if (chapterPages.chaptersSize > 1) {
				// trim pages
				if (chapterPages.size > PAGES_TRIM_THRESHOLD) {
					if (isNext) {
						chapterPages.removeFirst()
					} else {
						chapterPages.removeLast()
					}
				}
			}
			if (isNext) {
				chapterPages.addLast(newChapter.id, newPages)
			} else {
				chapterPages.addFirst(newChapter.id, newPages)
			}
		}
		return true
	}

	suspend fun loadSingleChapter(chapterId: String): Boolean {
		val pages = loadChapter(chapterId)
		return mutex.withLock {
			chapterPages.clear()
			chapterPages.addLast(chapterId, pages)
			pages.isNotEmpty()
		}
	}

	fun peekChapter(chapterId: String): MangaChapter? = chapters[chapterId]

	fun hasPages(chapterId: String): Boolean {
		return chapterId in chapterPages
	}

	fun getPages(chapterId: String): List<MangaPage> = synchronized(chapterPages) {
		return chapterPages.subList(chapterId).map { it.toMangaPage() }
	}

	fun getPagesCount(chapterId: String): Int {
		return chapterPages.size(chapterId)
	}

	fun last() = chapterPages.last()

	fun first() = chapterPages.first()

	fun snapshot() = chapterPages.toList()

	private suspend fun loadChapter(chapterId: String): List<ReaderPage> {
		val chapter = checkNotNull(chapters[chapterId]) { "Requested chapter not found" }
		val repo = mangaRepositoryFactory.create(com.nyora.hasan72341.core.model.MangaSource(currentMangaSource))
		return repo.getPages(chapter).mapIndexed { index, page ->
			ReaderPage(page, index, chapterId)
		}
	}
}
