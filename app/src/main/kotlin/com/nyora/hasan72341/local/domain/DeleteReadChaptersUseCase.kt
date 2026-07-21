package com.nyora.hasan72341.local.domain

import com.nyora.hasan72341.core.model.isLocal
import com.nyora.hasan72341.core.model.toMangaSource
import com.nyora.hasan72341.core.parser.MangaRepository
import com.nyora.hasan72341.core.util.ext.printStackTraceDebug
import com.nyora.hasan72341.history.data.HistoryRepository
import com.nyora.hasan72341.local.data.LocalMangaRepository
import com.nyora.hasan72341.local.domain.model.LocalManga
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.mihon.parsers.model.MangaChapter
import com.nyora.hasan72341.mihon.parsers.util.recoverCatchingCancellable
import com.nyora.hasan72341.mihon.parsers.util.runCatchingCancellable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

class DeleteReadChaptersUseCase @Inject constructor(
	private val localMangaRepository: LocalMangaRepository,
	private val historyRepository: HistoryRepository,
	private val mangaRepositoryFactory: MangaRepository.Factory,
) {

	suspend operator fun invoke(manga: Manga): Int {
		val localManga = if (manga.isLocal) {
			LocalManga(manga)
		} else {
			checkNotNull(localMangaRepository.findSavedManga(manga)) { "Cannot find local manga" }
		}
		val task = getDeletionTask(localManga) ?: return 0
		localMangaRepository.deleteChapters(task.manga.manga, task.chaptersIds)
		return task.chaptersIds.size
	}

	suspend operator fun invoke(): Int {
		val list = localMangaRepository.getList(0, null, null)
		if (list.isEmpty()) {
			return 0
		}
		return channelFlow {
			for (manga in list) {
				launch(Dispatchers.IO) {
					val task = runCatchingCancellable {
						getDeletionTask(LocalManga(manga))
					}.onFailure {
						it.printStackTraceDebug("DeleteReadChaptersUseCase::invoke")
					}.getOrNull()
					if (task != null) {
						send(task)
					}
				}
			}
		}.buffer().map {
			runCatchingCancellable {
				localMangaRepository.deleteChapters(it.manga.manga, it.chaptersIds)
				it.chaptersIds.size
			}.onFailure {
				it.printStackTraceDebug("DeleteReadChaptersUseCase::invoke")
			}.getOrDefault(0)
		}.fold(0) { acc, x -> acc + x }
	}

	private suspend fun getDeletionTask(manga: LocalManga): DeletionTask? {
		val history = historyRepository.getOne(manga.manga) ?: return null
		val chapters = getAllChapters(manga)
		if (chapters.isEmpty()) {
			return null
		}
		val historyChapterId = history.chapterId.toString()
		val branch = (chapters.find { it.id == historyChapterId } ?: return null).branch
		val filteredChapters = chapters.filter { x -> x.branch == branch }.takeWhile { it.id != historyChapterId }
		return if (filteredChapters.isEmpty()) {
			null
		} else {
			DeletionTask(
				manga = manga,
				chaptersIds = filteredChapters.mapTo(mutableSetOf()) { it.id },
			)
		}
	}

	private suspend fun getAllChapters(manga: LocalManga): List<MangaChapter> = runCatchingCancellable {
		val remoteManga = checkNotNull(localMangaRepository.getRemoteManga(manga.manga))
		checkNotNull(mangaRepositoryFactory.create(remoteManga.source.toMangaSource()).getDetails(remoteManga).chapters)
	}.recoverCatchingCancellable {
		checkNotNull(
			manga.manga.chapters.let {
				if (it.isNullOrEmpty()) {
					localMangaRepository.getDetails(manga.manga).chapters
				} else {
					it
				}
			},
		)
	}.getOrDefault(manga.manga.chapters.orEmpty())

	private class DeletionTask(
		val manga: LocalManga,
		val chaptersIds: Set<String>,
	)
}
