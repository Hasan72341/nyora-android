package com.nyora.hasan72341.tracker.domain

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.room.withTransaction
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import com.nyora.hasan72341.core.db.MangaDatabase
import com.nyora.hasan72341.core.db.entity.toManga
import com.nyora.hasan72341.core.parser.MangaRepository
import com.nyora.hasan72341.core.parser.ParserMangaRepository
import com.nyora.hasan72341.core.prefs.AppSettings
import com.nyora.hasan72341.core.util.ext.mapItems
import com.nyora.hasan72341.core.util.ext.toInstantOrNull
import com.nyora.hasan72341.details.domain.ProgressUpdateUseCase
import com.nyora.hasan72341.list.domain.ListFilterOption
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.mihon.parsers.util.ifZero
import com.nyora.hasan72341.tracker.data.TrackEntity
import com.nyora.hasan72341.tracker.data.TrackLogEntity
import com.nyora.hasan72341.tracker.data.toTrackingLogItem
import com.nyora.hasan72341.tracker.domain.model.MangaTracking
import com.nyora.hasan72341.tracker.domain.model.MangaUpdates
import com.nyora.hasan72341.tracker.domain.model.TrackingLogItem
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

private const val NO_ID = 0L
private const val NO_ID_STRING = ""
private const val MAX_LOG_SIZE = 120

@Reusable
class TrackingRepository @Inject constructor(
	private val db: MangaDatabase,
	private val settings: AppSettings,
	private val progressUpdateUseCase: ProgressUpdateUseCase,
	@ApplicationContext private val context: Context,
	private val mangaRepositoryFactory: MangaRepository.Factory,
) {

	private var isGcCalled = AtomicBoolean(false)

	suspend fun getNewChaptersCount(mangaId: Long): Int {
		return db.getTracksDao().findNewChapters(mangaId.toString())
	}

	fun observeNewChaptersCount(mangaId: Long): Flow<Int> {
		return db.getTracksDao().observeNewChapters(mangaId.toString())
	}

	@Deprecated("")
	fun observeUpdatedMangaCount(): Flow<Int> {
		return db.getTracksDao().observeUpdateMangaCount()
			.onStart { gcIfNotCalled() }
	}

	fun observeUnreadUpdatesCount(): Flow<Int> {
		return db.getTrackLogsDao().observeUnreadCount()
	}

	fun observeUpdatedManga(limit: Int, filterOptions: Set<ListFilterOption>): Flow<List<MangaTracking>> {
		return db.getTracksDao().observeUpdatedManga(limit, filterOptions)
			.mapItems {
				MangaTracking(
					manga = it.manga.toManga(),
					lastChapterId = it.track.lastChapterId.toLongOrNull() ?: NO_ID,
					lastCheck = it.track.lastCheckTime.toInstantOrNull(),
					lastChapterDate = it.track.lastChapterDate.toInstantOrNull(),
					newChapters = it.track.newChapters,
				)
			}.distinctUntilChanged()
			.onStart { gcIfNotCalled() }
	}

	suspend fun getTracks(offset: Int, limit: Int): List<MangaTracking> {
		return db.getTracksDao().findAll(offset = offset, limit = limit)
			.filter { track ->
				val manga = track.manga.toManga()
				!isUpdateCheckingDisabled(manga)
			}
			.map {
				MangaTracking(
					manga = it.manga.toManga(),
					lastChapterId = it.track.lastChapterId.toLongOrNull() ?: NO_ID,
					lastCheck = it.track.lastCheckTime.toInstantOrNull(),
					lastChapterDate = it.track.lastChapterDate.toInstantOrNull(),
					newChapters = it.track.newChapters,
				)
			}
	}

	private fun isUpdateCheckingDisabled(manga: Manga): Boolean {
		val repository = mangaRepositoryFactory.create(com.nyora.hasan72341.core.model.MangaSource(manga.source.name))
		if (repository !is ParserMangaRepository) {
			return false
		}
		// DisableUpdateChecking ConfigKey is not present in the native model; update checking stays enabled.
		return false
	}

	@Deprecated("")
	suspend fun getTrack(manga: Manga): MangaTracking {
		return getTrackOrNull(manga) ?: MangaTracking(
			manga = manga,
			lastChapterId = NO_ID,
			lastCheck = null,
			lastChapterDate = null,
			newChapters = 0,
		)
	}

	suspend fun getTrackOrNull(manga: Manga): MangaTracking? {
		val track = db.getTracksDao().find(manga.id) ?: return null
		return MangaTracking(
			manga = manga,
			lastChapterId = track.lastChapterId.toLongOrNull() ?: NO_ID,
			lastCheck = track.lastCheckTime.toInstantOrNull(),
			lastChapterDate = track.lastChapterDate.toInstantOrNull(),
			newChapters = track.newChapters,
		)
	}

	@VisibleForTesting
	suspend fun deleteTrack(mangaId: Long) {
		db.getTracksDao().delete(mangaId.toString())
	}

	fun observeTrackingLog(limit: Int, filterOptions: Set<ListFilterOption>): Flow<List<TrackingLogItem>> {
		return db.getTrackLogsDao().observeAll(limit, filterOptions)
			.mapItems { it.toTrackingLogItem() }
			.onStart { gcIfNotCalled() }
	}

	suspend fun getLogsCount() = db.getTrackLogsDao().count()

	suspend fun clearLogs() = db.getTrackLogsDao().clear()

	suspend fun clearCounters() = db.getTracksDao().clearCounters()

	suspend fun markAsRead(trackLogId: Long) = db.getTrackLogsDao().markAsRead(trackLogId)

	suspend fun gc() = db.withTransaction {
		db.getTracksDao().gc()
		db.getTrackLogsDao().run {
			gc()
			trim(MAX_LOG_SIZE)
		}
	}

	suspend fun saveUpdates(updates: MangaUpdates) {
		db.withTransaction {
			val track = getOrCreateTrack(updates.manga.id).mergeWith(updates)
			db.getTracksDao().upsert(track)
			if (updates is MangaUpdates.Success && updates.isValid && updates.newChapters.isNotEmpty()) {
				progressUpdateUseCase(updates.manga)
				val logEntity = TrackLogEntity(
					mangaId = updates.manga.id,
					chapters = updates.newChapters.joinToString("\n") { x -> x.title },
					createdAt = System.currentTimeMillis(),
					isUnread = true,
				)
				db.getTrackLogsDao().insert(logEntity)
			}
		}
	}

	suspend fun clearUpdates(ids: Collection<Long>) {
		when {
			ids.isEmpty() -> return
			ids.size == 1 -> db.getTracksDao().clearCounter(ids.single().toString())
			else -> db.withTransaction {
				for (id in ids) {
					db.getTracksDao().clearCounter(id.toString())
				}
			}
		}
	}

	suspend fun mergeWith(tracking: MangaTracking) {
		val entity = TrackEntity(
			mangaId = tracking.manga.id,
			lastChapterId = tracking.lastChapterId.toString(),
			newChapters = tracking.newChapters,
			lastCheckTime = tracking.lastCheck?.toEpochMilli() ?: 0L,
			lastChapterDate = tracking.lastChapterDate?.toEpochMilli() ?: 0L,
			lastResult = TrackEntity.RESULT_EXTERNAL_MODIFICATION,
			lastError = null,
		)
		db.getTracksDao().upsert(entity)
	}

	suspend fun getCategoriesCount(): IntArray {
		val categories = db.getFavouriteCategoriesDao().findAll()
		return intArrayOf(
			categories.count { it.track },
			categories.size,
		)
	}

	suspend fun updateTracks() = db.withTransaction {
		val dao = db.getTracksDao()
		dao.gc()
		val ids = dao.findAllIds().toMutableSet()
		val size = ids.size
		// history
		if (AppSettings.TRACK_HISTORY in settings.trackSources) {
			val historyIds = db.getHistoryDao().findAllIds()
			for (mangaId in historyIds) {
				if (!ids.remove(mangaId)) {
					dao.upsert(TrackEntity.create(mangaId))
				}
			}
		}
		// favorites
		if (AppSettings.TRACK_FAVOURITES in settings.trackSources) {
			val favoritesIds = db.getFavouritesDao().findIdsWithTrack()
			for (mangaId in favoritesIds) {
				if (!ids.remove(mangaId)) {
					dao.upsert(TrackEntity.create(mangaId))
				}
			}
		}
		// remove unused
		for (mangaId in ids) {
			dao.delete(mangaId)
		}
		size - ids.size
	}

	private suspend fun getOrCreateTrack(mangaId: String): TrackEntity {
		return db.getTracksDao().find(mangaId) ?: TrackEntity.create(mangaId)
	}

	private fun TrackEntity.mergeWith(updates: MangaUpdates): TrackEntity {
		return when (updates) {
			is MangaUpdates.Failure -> TrackEntity(
				mangaId = mangaId,
				lastChapterId = lastChapterId,
				newChapters = newChapters,
				lastCheckTime = System.currentTimeMillis(),
				lastChapterDate = lastChapterDate,
				lastResult = TrackEntity.RESULT_FAILED,
				lastError = updates.error?.toString(),
			)

			is MangaUpdates.Success -> TrackEntity(
				mangaId = mangaId,
				lastChapterId = updates.manga.chapters.lastOrNull()?.id ?: NO_ID_STRING,
				newChapters = if (updates.isValid) newChapters + updates.newChapters.size else 0,
				lastCheckTime = System.currentTimeMillis(),
				lastChapterDate = updates.lastChapterDate().ifZero { lastChapterDate },
				lastResult = if (updates.isNotEmpty()) TrackEntity.RESULT_HAS_UPDATE else TrackEntity.RESULT_NO_UPDATE,
				lastError = null,
			)
		}
	}

	private suspend fun gcIfNotCalled() {
		if (isGcCalled.compareAndSet(false, true)) {
			gc()
		}
	}
}
