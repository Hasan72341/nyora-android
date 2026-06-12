package com.nyora.hasan72341.local.domain

import com.nyora.hasan72341.core.model.isLocal
import com.nyora.hasan72341.core.util.ext.printStackTraceDebug
import com.nyora.hasan72341.history.data.HistoryRepository
import com.nyora.hasan72341.local.data.LocalMangaRepository
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.mihon.parsers.util.runCatchingCancellable
import java.io.IOException
import javax.inject.Inject

class DeleteLocalMangaUseCase @Inject constructor(
	private val localMangaRepository: LocalMangaRepository,
	private val historyRepository: HistoryRepository,
) {

	suspend operator fun invoke(manga: Manga) {
		val victim = if (manga.isLocal) manga else localMangaRepository.findSavedManga(manga)?.manga
		checkNotNull(victim) { "Cannot find saved manga for ${manga.title}" }
		val original = if (manga.isLocal) localMangaRepository.getRemoteManga(manga) else manga
		localMangaRepository.delete(victim) || throw IOException("Unable to delete file")
		runCatchingCancellable {
			historyRepository.deleteOrSwap(victim, original)
		}.onFailure {
			it.printStackTraceDebug("DeleteLocalMangaUseCase::invoke")
		}
	}

	suspend operator fun invoke(ids: Set<String>) {
		val list = localMangaRepository.getList(0, null, null)
		var removed = 0
		for (manga in list) {
			if (manga.id in ids) {
				invoke(manga)
				removed++
			}
		}
		check(removed == ids.size) {
			"Removed $removed files but ${ids.size} requested"
		}
	}
}
