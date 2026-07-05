package com.nyora.hasan72341.explore.data

import android.content.Context
import androidx.room.withTransaction
import com.nyora.hasan72341.BuildConfig
import com.nyora.hasan72341.core.LocalizedAppContext
import com.nyora.hasan72341.core.db.MangaDatabase
import com.nyora.hasan72341.core.db.dao.MangaSourcesDao
import com.nyora.hasan72341.core.db.entity.MangaSourceEntity
import com.nyora.hasan72341.core.model.MangaSourceInfo
import com.nyora.hasan72341.core.model.getTitle
import com.nyora.hasan72341.core.model.isNsfw
import com.nyora.hasan72341.core.model.getContentTypeOrNull
import com.nyora.hasan72341.core.prefs.AppSettings
import com.nyora.hasan72341.core.prefs.observeAsFlow
import com.nyora.hasan72341.core.ui.util.ReversibleHandle
import com.nyora.hasan72341.core.util.ext.flattenLatest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import com.nyora.hasan72341.mihon.parsers.model.ContentType
import com.nyora.hasan72341.mihon.parsers.model.MangaParserSource
import com.nyora.hasan72341.mihon.parsers.model.MangaSource
import com.nyora.hasan72341.mihon.parsers.network.CloudFlareHelper
import com.nyora.hasan72341.mihon.parsers.util.mapToSet
import java.util.HashSet
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangaSourcesRepository @Inject constructor(
    @LocalizedAppContext private val context: Context,
    private val db: MangaDatabase,
    private val settings: AppSettings,
) {

	private val isNewSourcesAssimilated = AtomicBoolean(false)
	private val dao: MangaSourcesDao
		get() = db.getSourcesDao()

	// Source catalog = the native kotatsu-parsers-redo sources
	// (the full MangaParserSource enum, minus entries flagged broken).
	val allMangaSources: Set<MangaSource>
		get() = MangaParserSource.entries.filterNotTo(HashSet()) { it.isBroken }

	suspend fun getEnabledSources(): List<MangaSource> {
		if (!settings.isSourcesUnlocked) return emptyList()
		assimilateNewSources()
		val order = settings.sourcesSortOrder
		return dao.findAll(!settings.isAllSourcesEnabled, order).toSources(settings.isNsfwContentDisabled, order)
	}

	suspend fun getPinnedSources(): Set<MangaSource> {
		if (!settings.isSourcesUnlocked) return emptySet()
		assimilateNewSources()
		val skipNsfw = settings.isNsfwContentDisabled
		return dao.findAllPinned()
			.mapNotNull { it.source.toMangaSourceOrNull()?.takeUnless { x -> skipNsfw && x.isNsfw() } }
			.toSet()
	}

	suspend fun getTopSources(limit: Int): List<MangaSource> {
		if (!settings.isSourcesUnlocked) return emptyList()
		assimilateNewSources()
		return dao.findLastUsed(limit).toSources(settings.isNsfwContentDisabled, null)
	}

	suspend fun getDisabledSources(): Set<MangaSource> {
		assimilateNewSources()
		if (settings.isAllSourcesEnabled) {
			return emptySet()
		}
		val result = HashSet<MangaSource>(allMangaSources)
		val enabled = dao.findAllEnabledNames()
		for (name in enabled) {
			val source = name.toMangaSourceOrNull() ?: continue
			result.remove(source)
		}
		return result
	}

	suspend fun queryParserSources(
		isDisabledOnly: Boolean,
		isNewOnly: Boolean,
		excludeBroken: Boolean,
		types: Set<ContentType>,
		query: String?,
		locale: String?,
		sortOrder: SourcesSortOrder?,
	): List<MangaSource> {
		if (!settings.isSourcesUnlocked) return emptyList()
		assimilateNewSources()
		val entities = dao.findAll().toMutableList()
		if (isDisabledOnly && !settings.isAllSourcesEnabled) {
			entities.removeAll { it.isEnabled }
		}
		if (isNewOnly) {
			entities.retainAll { it.addedIn == BuildConfig.VERSION_CODE }
		}
		val sources = entities.toSources(
			skipNsfwSources = settings.isNsfwContentDisabled,
			sortOrder = sortOrder,
		).run {
			mapTo(ArrayList(size)) { it.mangaSource }
		}

		if (locale != null) {
			sources.retainAll { it is MangaParserSource && it.locale == locale }
		}
		if (types.isNotEmpty()) {
			sources.retainAll { it.getContentTypeOrNull() in types }
		}
		if (!query.isNullOrEmpty()) {
			sources.retainAll {
				it.getTitle(context).contains(query, ignoreCase = true) || it.name.contains(query, ignoreCase = true)
			}
		}
		return sources
	}

	fun observeIsEnabled(source: MangaSource): Flow<Boolean> {
		return dao.observeIsEnabled(source.name).onStart { assimilateNewSources() }
	}

	fun observeEnabledSourcesCount(): Flow<Int> {
		return combine(
			observeSourcesUnlocked(),
			observeIsNsfwDisabled(),
			observeAllEnabled().flatMapLatest { isAllSourcesEnabled ->
				dao.observeAll(!isAllSourcesEnabled, SourcesSortOrder.MANUAL)
			},
		) { unlocked, skipNsfw, sources ->
			if (!unlocked) 0 else sources.count {
				it.source.toMangaSourceOrNull()?.let { s -> !skipNsfw || !s.isNsfw() } == true
			}
		}.distinctUntilChanged().onStart { assimilateNewSources() }
	}

	fun observeAvailableSourcesCount(): Flow<Int> {
		return combine(
			observeIsNsfwDisabled(),
			observeAllEnabled().flatMapLatest { isAllSourcesEnabled ->
				dao.observeAll(!isAllSourcesEnabled, SourcesSortOrder.MANUAL)
			},
		) { skipNsfw, enabledSources ->
			val enabled = enabledSources.mapToSet { it.source }
			allMangaSources.count { x ->
				x.name !in enabled && (!skipNsfw || !x.isNsfw())
			}
		}.distinctUntilChanged().onStart { assimilateNewSources() }
	}

	fun observeEnabledSources(): Flow<List<MangaSourceInfo>> = combine(
		observeIsNsfwDisabled(),
		observeAllEnabled(),
		observeSortOrder(),
		observeSourcesUnlocked(),
	) { skipNsfw, allEnabled, order, unlocked ->
		val sourcesFlow: Flow<List<MangaSourceInfo>> = if (!unlocked) {
			flowOf(emptyList())
		} else {
			dao.observeAll(!allEnabled, order).map {
				it.toSources(skipNsfw, order)
			}
		}
		sourcesFlow
	}.flattenLatest()
		.onStart { assimilateNewSources() }

	fun observeAll(): Flow<List<Pair<MangaSource, Boolean>>> = dao.observeAll().map { entities ->
		val result = ArrayList<Pair<MangaSource, Boolean>>(entities.size)
		for (entity in entities) {
			val source = entity.source.toMangaSourceOrNull() ?: continue
			if (source in allMangaSources) {
				result.add(source to entity.isEnabled)
			}
		}
		result
	}.onStart { assimilateNewSources() }

	suspend fun setSourcesEnabled(sources: Collection<MangaSource>, isEnabled: Boolean): ReversibleHandle {
		setSourcesEnabledImpl(sources, isEnabled)
		return ReversibleHandle {
			setSourcesEnabledImpl(sources, !isEnabled)
		}
	}

	suspend fun setSourcesEnabledExclusive(sources: Set<MangaSource>) {
		db.withTransaction {
			assimilateNewSources()
			for (s in allMangaSources) {
				dao.setEnabled(s.name, s in sources)
			}
		}
	}

	suspend fun disableAllSources() {
		db.withTransaction {
			assimilateNewSources()
			dao.disableAllSources()
		}
	}

	suspend fun setPositions(sources: List<MangaSource>) {
		db.withTransaction {
			for ((index, item) in sources.withIndex()) {
				dao.setSortKey(item.name, index)
			}
		}
	}

	fun observeHasNewSources(): Flow<Boolean> = observeIsNsfwDisabled().map { skipNsfw ->
		val sources = dao.findAllFromVersion(BuildConfig.VERSION_CODE).toSources(skipNsfw, null)
		sources.isNotEmpty() && sources.size != allMangaSources.size
	}.onStart { assimilateNewSources() }

	fun observeHasNewSourcesForBadge(): Flow<Boolean> = combine(
		settings.observeAsFlow(AppSettings.KEY_SOURCES_VERSION) { sourcesVersion },
		observeIsNsfwDisabled(),
	) { version, skipNsfw ->
		if (version < BuildConfig.VERSION_CODE) {
			val sources = dao.findAllFromVersion(version).toSources(skipNsfw, null)
			sources.isNotEmpty()
		} else {
			false
		}
	}.onStart { assimilateNewSources() }

	fun clearNewSourcesBadge() {
		settings.sourcesVersion = BuildConfig.VERSION_CODE
	}

	private suspend fun assimilateNewSources(): Boolean {
		if (isNewSourcesAssimilated.getAndSet(true)) {
			return false
		}
		val new = getNewSources()
		if (new.isEmpty()) {
			return false
		}
		var maxSortKey = dao.getMaxSortKey()
		val isAllEnabled = settings.isAllSourcesEnabled
		val entities = new.map { x ->
			MangaSourceEntity(
				source = x.name,
				isEnabled = isAllEnabled,
				sortKey = ++maxSortKey,
				addedIn = BuildConfig.VERSION_CODE,
				lastUsedAt = 0,
				isPinned = false,
				cfState = CloudFlareHelper.PROTECTION_NOT_DETECTED,
			)
		}
		dao.insertIfAbsent(entities)
		return true
	}

	suspend fun isSetupRequired(): Boolean {
		return settings.sourcesVersion == 0 && dao.findAll().isEmpty() && db.getMangaDao().getCount() == 0
	}

	suspend fun setIsPinned(sources: Collection<MangaSource>, isPinned: Boolean): ReversibleHandle {
		setSourcesPinnedImpl(sources, isPinned)
		return ReversibleHandle {
			setSourcesEnabledImpl(sources, !isPinned)
		}
	}

	suspend fun trackUsage(source: MangaSource) {
		if (!settings.isIncognitoModeEnabled(source.isNsfw())) {
			dao.setLastUsed(source.name, System.currentTimeMillis())
		}
	}

	private suspend fun setSourcesEnabledImpl(sources: Collection<MangaSource>, isEnabled: Boolean) {
		if (sources.size == 1) { // fast path
			dao.setEnabled(sources.first().name, isEnabled)
			return
		}
		db.withTransaction {
			for (source in sources) {
				dao.setEnabled(source.name, isEnabled)
			}
		}
	}

	private suspend fun getNewSources(): MutableSet<out MangaSource> {
		val entities = dao.findAll()
		val result = HashSet<MangaSource>()
		result.addAll(MangaParserSource.entries.filterNot { it.isBroken })
		for (e in entities) {
			result.remove(e.source.toMangaSourceOrNull() ?: continue)
		}
		return result
	}

	private suspend fun setSourcesPinnedImpl(sources: Collection<MangaSource>, isPinned: Boolean) {
		if (sources.size == 1) { // fast path
			dao.setPinned(sources.first().name, isPinned)
			return
		}
		db.withTransaction {
			for (source in sources) {
				dao.setPinned(source.name, isPinned)
			}
		}
	}

	private fun List<MangaSourceEntity>.toSources(
		skipNsfwSources: Boolean,
		sortOrder: SourcesSortOrder?,
	): MutableList<MangaSourceInfo> {
		val isAllEnabled = settings.isAllSourcesEnabled
		val result = ArrayList<MangaSourceInfo>(size)
		for (entity in this) {
			val source = entity.source.toMangaSourceOrNull() ?: continue
			if (skipNsfwSources && source.isNsfw()) {
				continue
			}
			if (source is MangaParserSource) {
				result.add(
					MangaSourceInfo(
						mangaSource = source,
						isEnabled = entity.isEnabled || isAllEnabled,
						isPinned = entity.isPinned,
					),
				)
			}
		}
		if (sortOrder == SourcesSortOrder.ALPHABETIC) {
			result.sortWith(compareBy<MangaSourceInfo> { !it.isPinned }.thenBy { it.getTitle(context) })
		}
		return result
	}

	private fun observeIsNsfwDisabled() = settings.observeAsFlow(AppSettings.KEY_DISABLE_NSFW) {
		isNsfwContentDisabled
	}

	private fun observeSortOrder() = settings.observeAsFlow(AppSettings.KEY_SOURCES_ORDER) {
		sourcesSortOrder
	}

	private fun observeAllEnabled() = settings.observeAsFlow(AppSettings.KEY_SOURCES_ENABLED_ALL) {
		isAllSourcesEnabled
	}

	// Remote master switch — when locked, the app exposes NO sources (see the
	// gated methods above). Flipped by a signed remote config on launch.
	private fun observeSourcesUnlocked() = settings.observeAsFlow(AppSettings.KEY_SOURCES_UNLOCKED) {
		isSourcesUnlocked
	}

	private fun String.toMangaSourceOrNull(): MangaSource? {
		if (startsWith("content:")) {
			return com.nyora.hasan72341.core.model.MangaSource(this)
		}
		// native kotatsu-parsers-redo sources are keyed by their enum name
		return MangaParserSource.entries.firstOrNull { it.name == this }
	}
}

private fun org.koitharu.kotatsu.parsers.model.ContentType.toNyoraContentType(): ContentType = when (this) {
	org.koitharu.kotatsu.parsers.model.ContentType.MANGA -> ContentType.MANGA
	org.koitharu.kotatsu.parsers.model.ContentType.MANHWA -> ContentType.MANHWA
	org.koitharu.kotatsu.parsers.model.ContentType.MANHUA -> ContentType.MANHUA
	org.koitharu.kotatsu.parsers.model.ContentType.HENTAI -> ContentType.HENTAI_MANGA
	org.koitharu.kotatsu.parsers.model.ContentType.COMICS -> ContentType.COMICS
	org.koitharu.kotatsu.parsers.model.ContentType.NOVEL -> ContentType.NOVEL
	org.koitharu.kotatsu.parsers.model.ContentType.ONE_SHOT -> ContentType.ONE_SHOT
	org.koitharu.kotatsu.parsers.model.ContentType.DOUJINSHI -> ContentType.DOUJINSHI
	org.koitharu.kotatsu.parsers.model.ContentType.IMAGE_SET -> ContentType.IMAGE_SET
	org.koitharu.kotatsu.parsers.model.ContentType.ARTIST_CG -> ContentType.ARTIST_CG
	org.koitharu.kotatsu.parsers.model.ContentType.GAME_CG -> ContentType.GAME_CG
	org.koitharu.kotatsu.parsers.model.ContentType.OTHER -> ContentType.OTHER
}
