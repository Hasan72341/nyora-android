package com.nyora.hasan72341.history.domain

import dagger.Reusable
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import com.nyora.hasan72341.core.parser.MangaRepository
import com.nyora.hasan72341.history.data.HistoryRepository
import com.nyora.hasan72341.core.model.toMangaSource
import com.nyora.hasan72341.mihon.parsers.model.Manga
import javax.inject.Inject

@Reusable
class MarkAsReadUseCase @Inject constructor(
	private val historyRepository: HistoryRepository,
	private val mangaRepositoryFactory: MangaRepository.Factory,
) {

	suspend operator fun invoke(manga: Manga) {
		val repo = mangaRepositoryFactory.create(manga.source.toMangaSource())
		val details = if (manga.chapters.isNullOrEmpty()) {
			repo.getDetails(manga)
		} else {
			manga
		}
		val lastChapter = checkNotNull(details.chapters).last()
		val pages = repo.getPages(lastChapter)
		historyRepository.addOrUpdate(
			manga = details,
			chapterId = lastChapter.id,
			page = pages.lastIndex,
			scroll = 0,
			percent = 1f,
			force = true,
		)
	}

	suspend operator fun invoke(manga: Collection<Manga>) {
		when (manga.size) {
			0 -> Unit
			1 -> invoke(manga.first())
			else -> supervisorScope {
				manga.map {
					launch {
						invoke(it)
					}
				}.joinAll()
			}
		}
	}
}
