package com.nyora.hasan72341.alternatives.domain

import androidx.room.withTransaction
import com.nyora.hasan72341.core.model.toMangaSource
import com.nyora.hasan72341.core.db.MangaDatabase
import com.nyora.hasan72341.core.model.getPreferredBranch
import com.nyora.hasan72341.core.parser.MangaDataRepository
import com.nyora.hasan72341.core.parser.MangaRepository
import com.nyora.hasan72341.details.domain.ProgressUpdateUseCase
import com.nyora.hasan72341.history.data.HistoryEntity
import com.nyora.hasan72341.history.data.toMangaHistory
import com.nyora.hasan72341.list.domain.ReadingProgress.Companion.PROGRESS_NONE
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.mihon.parsers.model.MangaChapter
import com.nyora.hasan72341.mihon.parsers.util.runCatchingCancellable
import com.nyora.hasan72341.scrobbling.common.domain.Scrobbler
import com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblingStatus
import com.nyora.hasan72341.tracker.data.TrackEntity
import javax.inject.Inject
import javax.inject.Provider

class MigrateUseCase
@Inject
constructor(
	private val mangaRepositoryFactory: MangaRepository.Factory,
	private val mangaDataRepository: MangaDataRepository,
	private val database: MangaDatabase,
	private val progressUpdateUseCase: ProgressUpdateUseCase,
	private val scrobblersProvider: Provider<Set<@JvmSuppressWildcards Scrobbler>>,
) {
	private val scrobblers: Set<@JvmSuppressWildcards Scrobbler> by lazy { scrobblersProvider.get() }
	suspend operator fun invoke(
		oldManga: Manga,
		newManga: Manga,
	) {
		val oldDetails = if (oldManga.chapters.isNullOrEmpty()) {
			runCatchingCancellable {
				mangaRepositoryFactory.create(oldManga.source.toMangaSource()).getDetails(oldManga)
			}.getOrDefault(oldManga)
		} else {
			oldManga
		}
		val newDetails = if (newManga.chapters.isNullOrEmpty()) {
			mangaRepositoryFactory.create(newManga.source.toMangaSource()).getDetails(newManga)
		} else {
			newManga
		}
		mangaDataRepository.storeManga(newDetails, replaceExisting = true)
		database.withTransaction {
			// replace favorites
			val favoritesDao = database.getFavouritesDao()
			val oldMangaId = oldDetails.id
			val newMangaId = newDetails.id
			val oldFavourites = favoritesDao.findAllRaw(oldMangaId)
			if (oldFavourites.isNotEmpty()) {
				favoritesDao.delete(oldMangaId)
				for (f in oldFavourites) {
					val e =
						f.copy(
							mangaId = newMangaId,
						)
					favoritesDao.upsert(e)
				}
			}
			// replace history
			val historyDao = database.getHistoryDao()
			val oldHistory = historyDao.find(oldMangaId)
			val newHistory =
				if (oldHistory != null) {
					val newHistory = makeNewHistory(oldDetails, newDetails, oldHistory)
					historyDao.delete(oldMangaId)
					historyDao.upsert(newHistory)
					newHistory
				} else {
					null
				}
			// track
			val tracksDao = database.getTracksDao()
			val oldTrack = tracksDao.find(oldMangaId)
			if (oldTrack != null) {
				val lastChapter = newDetails.chapters?.lastOrNull()
				val newTrack =
					TrackEntity(
						mangaId = newMangaId,
						lastChapterId = lastChapter?.id ?: "",
						newChapters = 0,
						lastCheckTime = System.currentTimeMillis(),
						lastChapterDate = lastChapter?.uploadDate ?: 0L,
						lastResult = TrackEntity.RESULT_EXTERNAL_MODIFICATION,
						lastError = null,
					)
				tracksDao.delete(oldMangaId)
				tracksDao.upsert(newTrack)
			}
			// scrobbling
			for (scrobbler in scrobblers) {
				if (!scrobbler.isEnabled) {
					continue
				}
				val prevInfo = scrobbler.getScrobblingInfoStub(oldMangaId) ?: continue
				scrobbler.unregisterScrobblingStub(oldMangaId)
				scrobbler.linkManga(newMangaId, prevInfo.targetId)
				scrobbler.updateScrobblingInfoStub(
					mangaId = newMangaId,
					rating = prevInfo.rating,
					status =
						prevInfo.status ?: when {
							newHistory == null -> ScrobblingStatus.PLANNED
							newHistory.percent == 1f -> ScrobblingStatus.COMPLETED
							else -> ScrobblingStatus.READING
						},
					comment = prevInfo.comment,
				)
				if (newHistory != null) {
					scrobbler.scrobble(
						manga = newDetails,
						chapterId = newHistory.chapterId,
					)
				}
			}
		}
		progressUpdateUseCase(newManga)
	}

	private fun makeNewHistory(
		oldManga: Manga,
		newManga: Manga,
		history: HistoryEntity,
	): HistoryEntity {
		if (oldManga.chapters.isNullOrEmpty()) { // probably broken manga/source
			val branch = newManga.getPreferredBranch(null)
			val chapters = newManga.chapters.orEmpty().filter { it.branch == branch }
			check(chapters.isNotEmpty()) { "Target manga has no chapters for branch $branch" }
			val currentChapter =
				if (history.percent in 0f..1f) {
					chapters[(chapters.lastIndex * history.percent).toInt()]
				} else {
					chapters.first()
				}
			return HistoryEntity(
				mangaId = newManga.id,
				createdAt = history.createdAt,
				updatedAt = history.updatedAt,
				chapterId = currentChapter.id,
				page = history.page,
				scroll = history.scroll,
				percent = history.percent,
				deletedAt = 0,
				chaptersCount = chapters.count { it.branch == currentChapter.branch },
			)
		}
		val branch = oldManga.getPreferredBranch(history.toMangaHistory())
		val oldChapters = oldManga.chapters.orEmpty().filter { it.branch == branch }
		check(oldChapters.isNotEmpty()) { "Source manga has no chapters for branch $branch" }
		var index = oldChapters.indexOfFirst { it.id == history.chapterId }
		if (index < 0) {
			index =
				if (history.percent in 0f..1f) {
					(oldChapters.lastIndex * history.percent).toInt()
				} else {
					0
				}
		}
		val newChapters = newManga.chapters.orEmpty().groupBy { it.branch }
		val newBranch =
			if (newChapters.containsKey(branch)) {
				branch
			} else {
				newManga.getPreferredBranch(null)
			}
		val newChapterId =
			checkNotNull(newChapters[newBranch])
				.let {
					val oldChapter = oldChapters[index]
					it.findByNumber(oldChapter.volume, oldChapter.number) ?: it.getOrNull(index) ?: it.last()
				}.id

		return HistoryEntity(
			mangaId = newManga.id,
			createdAt = history.createdAt,
			updatedAt = history.updatedAt,
			chapterId = newChapterId,
			page = history.page,
			scroll = history.scroll,
			percent = PROGRESS_NONE,
			deletedAt = 0,
			chaptersCount = checkNotNull(newChapters[newBranch]).size,
		)
	}

	private fun List<MangaChapter>.findByNumber(
		volume: Int,
		number: Float,
	): MangaChapter? =
		if (number <= 0f) {
			null
		} else {
			firstOrNull { it.volume == volume && it.number == number }
		}
}

// Minimal no-op stubs for scrobbling APIs that are no longer available end-to-end.
private fun Scrobbler.getScrobblingInfoStub(
	@Suppress("UNUSED_PARAMETER") mangaId: String,
): com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblingInfo? = null

@Suppress("UNUSED_PARAMETER")
private suspend fun Scrobbler.unregisterScrobblingStub(mangaId: String) = Unit

@Suppress("UNUSED_PARAMETER")
private suspend fun Scrobbler.updateScrobblingInfoStub(
	mangaId: String,
	rating: Float,
	status: ScrobblingStatus?,
	comment: String?,
) = Unit
