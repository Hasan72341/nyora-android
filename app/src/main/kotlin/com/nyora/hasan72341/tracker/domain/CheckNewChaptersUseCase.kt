package com.nyora.hasan72341.tracker.domain

import android.util.Log
import coil3.request.CachePolicy
import com.nyora.hasan72341.BuildConfig
import com.nyora.hasan72341.core.model.getPreferredBranch
import com.nyora.hasan72341.core.model.isLocal
import com.nyora.hasan72341.core.parser.CachingMangaRepository
import com.nyora.hasan72341.core.parser.MangaRepository
import com.nyora.hasan72341.core.util.MultiMutex
import com.nyora.hasan72341.core.util.ext.printStackTraceDebug
import com.nyora.hasan72341.core.util.ext.toInstantOrNull
import com.nyora.hasan72341.history.data.HistoryRepository
import com.nyora.hasan72341.local.data.LocalMangaRepository
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.mihon.parsers.util.findById
import com.nyora.hasan72341.mihon.parsers.util.runCatchingCancellable
import com.nyora.hasan72341.tracker.domain.model.MangaTracking
import com.nyora.hasan72341.tracker.domain.model.MangaUpdates
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckNewChaptersUseCase @Inject constructor(
	private val repository: TrackingRepository,
	private val historyRepository: HistoryRepository,
	private val mangaRepositoryFactory: MangaRepository.Factory,
	private val localMangaRepository: LocalMangaRepository,
) {

	private val mutex = MultiMutex<Long>()

	suspend operator fun invoke(manga: Manga): MangaUpdates = mutex.withLock(manga.id.toLong()) {
		repository.updateTracks()
		val tracking = repository.getTrackOrNull(manga) ?: return@withLock MangaUpdates.Failure(
			manga = manga,
			error = null,
		)
		invokeImpl(tracking)
	}

	suspend operator fun invoke(track: MangaTracking): MangaUpdates = mutex.withLock(track.manga.id.toLong()) {
		invokeImpl(track)
	}

	suspend operator fun invoke(manga: Manga, currentChapterId: Long) = mutex.withLock(manga.id.toLong()) {
		runCatchingCancellable {
			repository.updateTracks()
			val details = getFullManga(manga)
			val track = repository.getTrackOrNull(manga) ?: return@withLock
			val branch = checkNotNull(details.chapters.find { it.id == currentChapterId.toString() }).branch
			val chapters = details.chapters.filter { it.branch == branch }
			val chapterIndex = chapters.indexOfFirst { x -> x.id == currentChapterId.toString() }
			val lastNewChapterIndex = chapters.size - track.newChapters
			val lastChapter = chapters.lastOrNull()
			val tracking = MangaTracking(
				manga = details,
				lastChapterId = lastChapter?.id?.toLongOrNull() ?: 0L,
				lastCheck = Instant.now(),
				lastChapterDate = lastChapter?.uploadDate?.toInstantOrNull() ?: track.lastChapterDate,
				newChapters = when {
					track.newChapters == 0 -> 0
					chapterIndex < 0 -> track.newChapters
					chapterIndex >= lastNewChapterIndex -> chapters.lastIndex - chapterIndex
					else -> track.newChapters
				},
			)
			repository.mergeWith(tracking)
		}.onFailure { e ->
			e.printStackTraceDebug("CheckNewChaptersUseCase::invoke")
		}.isSuccess
	}

	private suspend fun invokeImpl(track: MangaTracking): MangaUpdates = runCatchingCancellable {
		val details = getFullManga(track.manga)
		compare(track, details, getBranch(details, track.lastChapterId))
	}.getOrElse { error ->
		MangaUpdates.Failure(
			manga = track.manga,
			error = error,
		)
	}.also { updates ->
		repository.saveUpdates(updates)
	}

	private suspend fun getBranch(manga: Manga, trackChapterId: Long): String? {
		historyRepository.getOne(manga)?.let { history ->
			manga.chapters.find { it.id == history.chapterId.toString() }
		}?.let {
			return it.branch
		}
		manga.chapters.find { it.id == trackChapterId.toString() }?.let {
			return it.branch
		}
		// fallback
		return manga.getPreferredBranch(null)
	}

	private suspend fun getFullManga(manga: Manga): Manga = when {
		manga.isLocal -> fetchDetails(
			requireNotNull(localMangaRepository.getRemoteManga(manga)) {
				"Local manga is not supported"
			},
		)

		manga.chapters.isEmpty() -> fetchDetails(manga)
		else -> manga
	}

	private suspend fun fetchDetails(manga: Manga): Manga {
		val repo = mangaRepositoryFactory.create(com.nyora.hasan72341.core.model.MangaSource(manga.source.name))
		return if (repo is CachingMangaRepository) {
			repo.getDetails(manga, CachePolicy.WRITE_ONLY)
		} else {
			repo.getDetails(manga)
		}
	}

	/**
	 * The main functionality of tracker: check new chapters in [manga] comparing to the [track]
	 */
	private fun compare(track: MangaTracking, manga: Manga, branch: String?): MangaUpdates.Success {
		if (track.isEmpty()) {
			// first check or manga was empty on last check
			return MangaUpdates.Success(manga, branch, emptyList(), isValid = false)
		}
		val chapters = manga.chapters.filter { it.branch == branch }
		if (BuildConfig.DEBUG && chapters.find { it.id == track.lastChapterId.toString() } == null) {
			Log.e("Tracker", "Chapter ${track.lastChapterId} not found")
		}
		val newChapters = chapters.takeLastWhile { x -> x.id != track.lastChapterId.toString() }
		return when {
			newChapters.isEmpty() -> {
				MangaUpdates.Success(
					manga = manga,
					branch = branch,
					newChapters = emptyList(),
					isValid = chapters.lastOrNull()?.id == track.lastChapterId.toString(),
				)
			}

			newChapters.size == chapters.size -> {
				MangaUpdates.Success(manga, branch, emptyList(), isValid = false)
			}

			else -> {
				MangaUpdates.Success(manga, branch, newChapters, isValid = true)
			}
		}
	}
}
