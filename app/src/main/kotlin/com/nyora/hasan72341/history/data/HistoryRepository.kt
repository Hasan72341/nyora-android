package com.nyora.hasan72341.history.data

import android.content.Context
import androidx.room.withTransaction
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.nyora.hasan72341.core.db.MangaDatabase
import com.nyora.hasan72341.core.db.entity.toEntity
import com.nyora.hasan72341.core.db.entity.toManga
import com.nyora.hasan72341.core.db.entity.toMangaList
import com.nyora.hasan72341.core.model.MangaHistory
import com.nyora.hasan72341.core.model.isLocal
import com.nyora.hasan72341.core.model.isNsfw
import com.nyora.hasan72341.core.model.toMangaSources
import com.nyora.hasan72341.core.parser.MangaDataRepository
import com.nyora.hasan72341.core.prefs.AppSettings
import com.nyora.hasan72341.core.prefs.ProgressIndicatorMode
import com.nyora.hasan72341.core.ui.util.ReversibleHandle
import com.nyora.hasan72341.core.util.ext.mapItems
import com.nyora.hasan72341.history.domain.model.MangaWithHistory
import com.nyora.hasan72341.list.domain.ListFilterOption
import com.nyora.hasan72341.list.domain.ListSortOrder
import com.nyora.hasan72341.list.domain.ReadingProgress
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.mihon.parsers.model.MangaSource
import com.nyora.hasan72341.mihon.parsers.model.MangaTag
import com.nyora.hasan72341.mihon.parsers.util.levenshteinDistance
import com.nyora.hasan72341.scrobbling.common.domain.Scrobbler
import com.nyora.hasan72341.scrobbling.common.domain.tryScrobble
import com.nyora.hasan72341.search.domain.SearchKind
import com.nyora.hasan72341.sync.supabase.SupabaseConfig
import com.nyora.hasan72341.sync.supabase.SupabaseSyncWorker
import com.nyora.hasan72341.tracker.domain.CheckNewChaptersUseCase
import javax.inject.Inject
import javax.inject.Provider

@Reusable
class HistoryRepository @Inject constructor(
	@ApplicationContext private val context: Context,
	private val db: MangaDatabase,
	private val settings: AppSettings,
	private val supabaseConfig: SupabaseConfig,
	private val scrobblersProvider: Provider<Set<@JvmSuppressWildcards Scrobbler>>,
	private val mangaRepository: MangaDataRepository,
	private val localObserver: HistoryLocalObserver,
	private val newChaptersUseCaseProvider: Provider<CheckNewChaptersUseCase>,
) {
	private val scrobblers: Set<@JvmSuppressWildcards Scrobbler> by lazy { scrobblersProvider.get() }

	suspend fun getList(offset: Int, limit: Int): List<Manga> {
		val entities = db.getHistoryDao().findAll(offset, limit)
		return entities.map { it.toManga() }
	}

	suspend fun search(query: String, kind: SearchKind, limit: Int): List<Manga> {
		val dao = db.getHistoryDao()
		val q = "%$query%"
		val entities = when (kind) {
			SearchKind.SIMPLE,
			SearchKind.TITLE -> dao.searchByTitle(q, limit).sortedBy { it.title.levenshteinDistance(query) }

			SearchKind.AUTHOR -> dao.searchByAuthor(q, limit)
			SearchKind.TAG -> dao.searchByTag(q, limit)
		}
		return entities.toMangaList()
	}

	suspend fun getLastOrNull(): Manga? {
		val entity = db.getHistoryDao().findAll(0, 1).firstOrNull() ?: return null
		return entity.toManga()
	}

	fun observeLast(): Flow<Manga?> {
		return db.getHistoryDao().observeAll(1).map {
			val first = it.firstOrNull()
			first?.toManga()
		}
	}

	fun observeAll(): Flow<List<Manga>> {
		return db.getHistoryDao().observeAll().mapItems {
			it.toManga()
		}
	}

	fun observeAll(limit: Int): Flow<List<Manga>> {
		return db.getHistoryDao().observeAll(limit).mapItems {
			it.toManga()
		}
	}

	fun observeAllWithHistory(
		order: ListSortOrder,
		filterOptions: Set<ListFilterOption>,
		limit: Int
	): Flow<List<MangaWithHistory>> {
		if (ListFilterOption.Downloaded in filterOptions) {
			return localObserver.observeAll(order, filterOptions, limit)
		}
		return db.getHistoryDao().observeAll(order, filterOptions, limit).mapItems {
			MangaWithHistory(
				it.toManga(),
				it.history.toMangaHistory(),
			)
		}
	}

	fun observeOne(id: String): Flow<MangaHistory?> {
		return db.getHistoryDao().observe(id).map {
			it?.toMangaHistory()
		}
	}

	suspend fun addOrUpdate(manga: Manga, chapterId: String, page: Int, scroll: Int, percent: Float, force: Boolean) {
		if (!force && shouldSkip(manga)) {
			return
		}
		val mangaId = manga.id
		if (mangaId.isBlank()) return
		db.withTransaction {
			mangaRepository.storeManga(manga, replaceExisting = true)
			val branch = manga.findChapterById(chapterId)?.branch
			db.getHistoryDao().upsert(
				HistoryEntity(
					mangaId = mangaId,
					createdAt = System.currentTimeMillis(),
					updatedAt = System.currentTimeMillis(),
					chapterId = chapterId,
					page = page,
					scroll = scroll.toFloat(), // we migrate to int, but decide to not update database
					percent = percent,
					chaptersCount = manga.chapters.count { it.branch == branch },
					deletedAt = 0L,
				),
			)
			newChaptersUseCaseProvider.get()(manga, chapterId.toLongOrNull() ?: 0L)
			scrobblers.forEach { it.tryScrobble(manga, chapterId) }
		}
		enqueueSyncAfterMutation()
	}

	suspend fun getOne(manga: Manga): MangaHistory? {
		val mangaId = manga.id
		if (mangaId.isBlank()) return null
		return db.getHistoryDao().find(mangaId)?.recoverIfNeeded(manga)?.toMangaHistory()
	}

	suspend fun getProgress(mangaId: String, mode: ProgressIndicatorMode): ReadingProgress? {
		val entity = db.getHistoryDao().find(mangaId) ?: return null
		val fixedPercent = if (ReadingProgress.isCompleted(entity.percent)) 1f else entity.percent
		return ReadingProgress(
			percent = fixedPercent,
			totalChapters = entity.chaptersCount,
			mode = mode,
		).takeIf { it.isValid() }
	}

	suspend fun clear() {
		db.getHistoryDao().clear()
		enqueueSyncAfterMutation()
	}

	suspend fun delete(manga: Manga) = db.withTransaction {
		db.getHistoryDao().delete(manga.id)
		mangaRepository.gcChaptersCache()
	}.also { enqueueSyncAfterMutation() }

	suspend fun deleteAfter(minDate: Long) = db.withTransaction {
		db.getHistoryDao().deleteAfter(minDate)
		mangaRepository.gcChaptersCache()
	}.also { enqueueSyncAfterMutation() }

	suspend fun deleteNotFavorite() = db.withTransaction {
		db.getHistoryDao().deleteNotFavorite()
		mangaRepository.gcChaptersCache()
	}.also { enqueueSyncAfterMutation() }

	suspend fun delete(ids: Collection<String>): ReversibleHandle {
		db.withTransaction {
			for (id in ids) {
				db.getHistoryDao().delete(id)
			}
			mangaRepository.gcChaptersCache()
		}
		enqueueSyncAfterMutation()
		return ReversibleHandle {
			recover(ids)
		}
	}

	/**
	 * Try to replace one manga with another one
	 * Useful for replacing saved manga on deleting it with remote source
	 */
	suspend fun deleteOrSwap(manga: Manga, alternative: Manga?) {
		if (alternative == null || db.getMangaDao().update(alternative.toEntity()) <= 0) {
			delete(manga)
		}
	}

	suspend fun getPopularTags(limit: Int): List<MangaTag> {
		return emptyList()
	}

	suspend fun getPopularSources(limit: Int): List<MangaSource> {
		return db.getHistoryDao().findPopularSources(limit).toMangaSources()
	}

	fun shouldSkip(manga: Manga): Boolean = settings.isIncognitoModeEnabled(manga.isNsfw())

	fun observeShouldSkip(manga: Manga): Flow<Boolean> {
		return settings.observe(AppSettings.KEY_INCOGNITO_MODE, AppSettings.KEY_INCOGNITO_NSFW)
			.map { shouldSkip(manga) }
			.distinctUntilChanged()
	}

	private suspend fun recover(ids: Collection<String>) {
		db.withTransaction {
			for (id in ids) {
				db.getHistoryDao().recover(id)
			}
		}
		enqueueSyncAfterMutation()
	}

	private suspend fun HistoryEntity.recoverIfNeeded(manga: Manga): HistoryEntity {
		val chapters = manga.chapters
		if (manga.isLocal || chapters.isNullOrEmpty() || manga.findChapterById(chapterId) != null) {
			return this
		}
		val newChapterId = chapters.getOrNull(
			(chapters.size * percent).toInt(),
		)?.id ?: return this
		val newEntity = copy(chapterId = newChapterId)
		db.getHistoryDao().update(newEntity)
		enqueueSyncAfterMutation()
		return newEntity
	}

	private fun enqueueSyncAfterMutation() {
		if (supabaseConfig.isAuthenticated) {
			SupabaseSyncWorker.enqueue(context)
		}
	}

	private fun HistoryWithManga.toManga() = manga.toManga()
}
