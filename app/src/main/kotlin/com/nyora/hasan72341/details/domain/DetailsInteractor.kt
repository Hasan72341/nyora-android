package com.nyora.hasan72341.details.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import com.nyora.hasan72341.core.model.FavouriteCategory
import com.nyora.hasan72341.core.model.isNsfw
import com.nyora.hasan72341.core.prefs.AppSettings
import com.nyora.hasan72341.core.prefs.TriStateOption
import com.nyora.hasan72341.core.prefs.observeAsFlow
import com.nyora.hasan72341.details.data.MangaDetails
import com.nyora.hasan72341.favourites.domain.FavouritesRepository
import com.nyora.hasan72341.history.data.HistoryRepository
import com.nyora.hasan72341.local.data.LocalMangaRepository
import com.nyora.hasan72341.local.domain.model.LocalManga
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.mihon.parsers.util.runCatchingCancellable
import com.nyora.hasan72341.scrobbling.common.domain.Scrobbler
import com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblingInfo
import com.nyora.hasan72341.tracker.domain.TrackingRepository
import javax.inject.Inject
import javax.inject.Provider

/* TODO: remove */
class DetailsInteractor @Inject constructor(
	private val historyRepository: HistoryRepository,
	private val favouritesRepository: FavouritesRepository,
	private val localMangaRepository: LocalMangaRepository,
	private val trackingRepository: TrackingRepository,
	private val settings: AppSettings,
	private val scrobblersProvider: Provider<Set<@JvmSuppressWildcards Scrobbler>>,
) {
	private val scrobblers: Set<@JvmSuppressWildcards Scrobbler> by lazy { scrobblersProvider.get() }

	fun observeFavourite(mangaId: String): Flow<Set<FavouriteCategory>> {
		return favouritesRepository.observeCategories(mangaId)
	}

	fun observeNewChapters(mangaId: String): Flow<Int> {
		val numericId = mangaId.toLongOrNull() ?: return flowOf(0)
		return settings.observeAsFlow(AppSettings.KEY_TRACKER_ENABLED) { isTrackerEnabled }
			.flatMapLatest { isEnabled ->
				if (isEnabled) {
					trackingRepository.observeNewChaptersCount(numericId)
				} else {
					flowOf(0)
				}
			}
	}

	fun observeScrobblingInfo(mangaId: String): Flow<List<ScrobblingInfo>> {
		// Scrobbling info API stubbed out; return empty.
		return flowOf(emptyList())
	}

	fun observeIncognitoMode(mangaFlow: Flow<Manga?>): Flow<TriStateOption> {
		return mangaFlow
			.filterNotNull()
			.distinctUntilChangedBy { it.isNsfw() }
			.combine(observeIncognitoMode()) { manga, globalIncognito ->
				when {
					globalIncognito -> TriStateOption.ENABLED
					manga.isNsfw() -> settings.incognitoModeForNsfw
					else -> TriStateOption.DISABLED
				}
			}
	}

	suspend fun updateLocal(subject: MangaDetails?, localManga: LocalManga): MangaDetails? {
		subject ?: return null
		return if (subject.id.toString() == localManga.manga.id) {
			if (subject.isLocal) {
				subject.copy(
					manga = localManga.manga,
				)
			} else {
				subject.copy(
					localManga = runCatchingCancellable {
						localManga.copy(
							manga = localMangaRepository.getDetails(localManga.manga),
						)
					}.getOrNull() ?: subject.local,
				)
			}
		} else {
			subject
		}
	}

	suspend fun findRemote(seed: Manga) = localMangaRepository.getRemoteManga(seed)

	private fun observeIncognitoMode() = settings.observeAsFlow(AppSettings.KEY_INCOGNITO_MODE) {
		isIncognitoModeEnabled
	}
}
