package com.nyora.hasan72341.details.domain

import com.nyora.hasan72341.core.db.MangaDatabase
import com.nyora.hasan72341.core.model.findChapterById
import com.nyora.hasan72341.core.model.getChapters
import com.nyora.hasan72341.core.model.isLocal
import com.nyora.hasan72341.core.model.toMangaSource
import com.nyora.hasan72341.core.os.NetworkState
import com.nyora.hasan72341.core.parser.MangaRepository
import com.nyora.hasan72341.list.domain.ReadingProgress.Companion.PROGRESS_NONE
import com.nyora.hasan72341.local.data.LocalMangaRepository
import com.nyora.hasan72341.mihon.parsers.model.Manga
import javax.inject.Inject

class ProgressUpdateUseCase @Inject constructor(
	private val mangaRepositoryFactory: MangaRepository.Factory,
	private val database: MangaDatabase,
	private val localMangaRepository: LocalMangaRepository,
	private val networkState: NetworkState,
) {

	suspend operator fun invoke(manga: Manga): Float {
		val history = database.getHistoryDao().find(manga.id) ?: return PROGRESS_NONE
		val seed = if (manga.isLocal) {
			localMangaRepository.getRemoteManga(manga) ?: manga
		} else {
			manga
		}
		if (!seed.isLocal && !networkState.value) {
			return PROGRESS_NONE
		}
		val repo = mangaRepositoryFactory.create(seed.source.toMangaSource())
		val details = if (manga.source != seed.source || seed.chapters.isNullOrEmpty()) {
			repo.getDetails(seed)
		} else {
			seed
		}
		val chapter = details.findChapterById(history.chapterId) ?: return PROGRESS_NONE
		val chapters = details.getChapters(chapter.branch)
		val chapterRepo = repo
		val chaptersCount = chapters.size
		if (chaptersCount == 0) {
			return PROGRESS_NONE
		}
		val chapterIndex = chapters.indexOfFirst { x -> x.id == history.chapterId }
		val pagesCount = chapterRepo.getPages(chapter).size
		if (pagesCount == 0) {
			return PROGRESS_NONE
		}
		val pagePercent = (history.page + 1) / pagesCount.toFloat()
		val ppc = 1f / chaptersCount
		val result = ppc * chapterIndex + ppc * pagePercent
		if (result != history.percent) {
			database.getHistoryDao().update(
				history.copy(
					chapterId = chapter.id,
					percent = result,
				),
			)
		}
		return result
	}
}
