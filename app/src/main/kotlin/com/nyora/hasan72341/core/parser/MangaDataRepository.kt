package com.nyora.hasan72341.core.parser

import androidx.collection.LongObjectMap
import androidx.collection.MutableLongObjectMap
import androidx.core.net.toUri
import androidx.room.withTransaction
import dagger.Reusable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.nyora.hasan72341.core.db.MangaDatabase
import com.nyora.hasan72341.core.db.TABLE_FAVOURITES
import com.nyora.hasan72341.core.db.TABLE_FAVOURITE_CATEGORIES
import com.nyora.hasan72341.core.db.TABLE_PREFERENCES
import com.nyora.hasan72341.core.db.entity.ContentRating
import com.nyora.hasan72341.core.db.entity.MangaPrefsEntity
import com.nyora.hasan72341.core.db.entity.toEntity
import com.nyora.hasan72341.core.db.entity.toManga
import com.nyora.hasan72341.core.model.LocalMangaSource
import com.nyora.hasan72341.core.model.isLocal
import com.nyora.hasan72341.core.nav.MangaIntent
import com.nyora.hasan72341.core.os.AppShortcutManager
import com.nyora.hasan72341.core.prefs.ReaderMode
import com.nyora.hasan72341.core.ui.model.MangaOverride
import com.nyora.hasan72341.core.util.ext.toFileOrNull
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.mihon.parsers.model.MangaSource
import com.nyora.hasan72341.mihon.parsers.model.MangaTag
import com.nyora.hasan72341.mihon.parsers.util.nullIfEmpty
import com.nyora.hasan72341.reader.domain.ReaderColorFilter
import javax.inject.Inject
import javax.inject.Provider

@Reusable
class MangaDataRepository @Inject constructor(
	private val db: MangaDatabase,
	private val resolverProvider: Provider<MangaLinkResolver>,
	private val appShortcutManagerProvider: Provider<AppShortcutManager>,
) {

	suspend fun saveReaderMode(manga: Manga, mode: ReaderMode) {
		db.withTransaction {
			storeManga(manga, replaceExisting = false)
			val mangaId = manga.id
			val entity = db.getPreferencesDao().find(mangaId) ?: newEntity(mangaId)
			db.getPreferencesDao().upsert(entity.copy(mode = mode.id))
		}
	}

	suspend fun saveColorFilter(manga: Manga, colorFilter: ReaderColorFilter?) {
		db.withTransaction {
			storeManga(manga, replaceExisting = false)
			val mangaId = manga.id
			val entity = db.getPreferencesDao().find(mangaId) ?: newEntity(mangaId)
			db.getPreferencesDao().upsert(
				entity.copy(
					cfBrightness = colorFilter?.brightness ?: 0f,
					cfContrast = colorFilter?.contrast ?: 0f,
					cfInvert = colorFilter?.isInverted == true,
					cfGrayscale = colorFilter?.isGrayscale == true,
					cfBookEffect = colorFilter?.isBookBackground == true,
					cfMultitone = colorFilter?.multitonePreset?.id ?: ReaderColorFilter.Preset.NONE.id,
				),
			)
		}
	}

	suspend fun resetColorFilters() {
		db.getPreferencesDao().resetColorFilters()
	}

	suspend fun getReaderMode(mangaId: String): ReaderMode? {
		return db.getPreferencesDao().find(mangaId)?.let { ReaderMode.valueOf(it.mode) }
	}

	suspend fun getColorFilter(mangaId: String): ReaderColorFilter? {
		return db.getPreferencesDao().find(mangaId)?.getColorFilterOrNull()
	}

	suspend fun getOverride(mangaId: String): MangaOverride? {
		return db.getPreferencesDao().find(mangaId)?.getOverrideOrNull()
	}

	suspend fun getOverrides(): Map<String, MangaOverride> {
		val entities = db.getPreferencesDao().getOverrides()
		val map = mutableMapOf<String, MangaOverride>()
		for (entity in entities) {
			map[entity.mangaId] = entity.getOverrideOrNull() ?: continue
		}
		return map
	}

	suspend fun setOverride(manga: Manga, override: MangaOverride?) {
		db.withTransaction {
			storeManga(manga, replaceExisting = false)
			val dao = db.getPreferencesDao()
			val mangaId = manga.id
			val entity = dao.find(mangaId) ?: newEntity(mangaId)
			dao.upsert(
				entity.copy(
					titleOverride = override?.title?.nullIfEmpty(),
					coverUrlOverride = override?.coverUrl?.nullIfEmpty(),
					contentRatingOverride = override?.contentRating?.name,
				),
			)
		}
	}

	fun observeColorFilter(mangaId: String): Flow<ReaderColorFilter?> {
		return db.getPreferencesDao().observe(mangaId)
			.map { it?.getColorFilterOrNull() }
			.distinctUntilChanged()
	}

	suspend fun findMangaById(mangaId: String, withChapters: Boolean): Manga? {
		return db.getMangaDao().find(mangaId)?.toManga()
	}

	suspend fun findMangaByPublicUrl(publicUrl: String): Manga? {
		return db.getMangaDao().findByPublicUrl(publicUrl)?.toManga()
	}

	suspend fun resolveIntent(intent: MangaIntent, withChapters: Boolean): Manga? = when {
		intent.manga != null -> intent.manga.withCachedChaptersIfNeeded(withChapters)
		intent.mangaId != null -> findMangaById(intent.mangaId!!, withChapters)
		intent.uri != null -> resolverProvider.get().resolve(intent.uri).withCachedChaptersIfNeeded(withChapters)
		else -> null
	}

	suspend fun storeManga(manga: Manga, replaceExisting: Boolean) {
		val mangaId = manga.id
		if (!replaceExisting && db.getMangaDao().find(mangaId) != null) {
			return
		}
		db.withTransaction {
			// avoid storing local manga if remote one is already stored
			val existing = if (manga.isLocal) {
				db.getMangaDao().find(mangaId)
			} else {
				null
			}
			if (existing == null || existing.source == manga.source.name) {
				db.getMangaDao().upsert(manga.toEntity())
			}
		}
	}

	suspend fun updateChapters(manga: Manga) {
		val mangaId = manga.id
		if (mangaId in db.getMangaDao()) {
			db.getMangaDao().upsert(manga.toEntity())
		}
	}

	suspend fun gcChaptersCache() {
		Unit
	}

	suspend fun findTags(source: MangaSource): Set<MangaTag> {
		return emptySet()
	}

	suspend fun cleanupLocalManga() {
		val dao = db.getMangaDao()
		val broken = dao.findAllBySource(LocalMangaSource.name)
			.filter { entity -> entity.url.toUri().toFileOrNull()?.exists() == false }
		if (broken.isNotEmpty()) {
			dao.delete(broken)
		}
	}

	suspend fun cleanupDatabase() {
		db.withTransaction {
			gcChaptersCache()
			val idsFromShortcuts = appShortcutManagerProvider.get().getMangaShortcuts().map { it.toString() }.toSet()
			db.getMangaDao().cleanup(idsFromShortcuts)
		}
	}

	fun observeOverridesTrigger(emitInitialState: Boolean) = db.invalidationTracker.createFlow(
		tables = arrayOf(TABLE_PREFERENCES),
		emitInitialState = emitInitialState,
	)

	fun observeFavoritesTrigger(emitInitialState: Boolean) = db.invalidationTracker.createFlow(
		tables = arrayOf(TABLE_FAVOURITES, TABLE_FAVOURITE_CATEGORIES),
		emitInitialState = emitInitialState,
	)

	private suspend fun Manga.withCachedChaptersIfNeeded(flag: Boolean): Manga = if (flag && !isLocal && chapters.isEmpty()) {
		db.getMangaDao().find(id)?.toManga() ?: this
	} else {
		this
	}

	private fun MangaPrefsEntity.getColorFilterOrNull(): ReaderColorFilter? {
		return if (cfBrightness != 0f || cfContrast != 0f || cfInvert || cfGrayscale || cfBookEffect || cfMultitone != ReaderColorFilter.Preset.NONE.id) {
			ReaderColorFilter(
				brightness = cfBrightness,
				contrast = cfContrast,
				isInverted = cfInvert,
				isGrayscale = cfGrayscale,
				isBookBackground = cfBookEffect,
				multitonePreset = ReaderColorFilter.Preset.fromId(cfMultitone),
			)
		} else {
			null
		}
	}

	private fun MangaPrefsEntity.getOverrideOrNull(): MangaOverride? {
		return if (titleOverride.isNullOrEmpty() && coverUrlOverride.isNullOrEmpty() && contentRatingOverride.isNullOrEmpty()) {
			null
		} else {
			MangaOverride(
				coverUrl = coverUrlOverride?.nullIfEmpty(),
				title = titleOverride?.nullIfEmpty(),
				contentRating = ContentRating(contentRatingOverride),
			)
		}
	}

	private fun newEntity(mangaId: String) = MangaPrefsEntity(
		mangaId = mangaId,
		mode = -1,
		cfBrightness = ReaderColorFilter.EMPTY.brightness,
		cfContrast = ReaderColorFilter.EMPTY.contrast,
		cfInvert = ReaderColorFilter.EMPTY.isInverted,
		cfGrayscale = ReaderColorFilter.EMPTY.isGrayscale,
		cfBookEffect = ReaderColorFilter.EMPTY.isBookBackground,
		cfMultitone = ReaderColorFilter.EMPTY.multitonePreset.id,
		titleOverride = null,
		coverUrlOverride = null,
		contentRatingOverride = null,
	)
}
