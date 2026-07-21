package com.nyora.hasan72341.list.domain

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.IntDef
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.parser.MangaDataRepository
import com.nyora.hasan72341.core.prefs.AppSettings
import com.nyora.hasan72341.core.prefs.ListMode
import com.nyora.hasan72341.core.ui.model.MangaOverride
import com.nyora.hasan72341.core.ui.widgets.ChipsView
import com.nyora.hasan72341.favourites.domain.FavouritesRepository
import com.nyora.hasan72341.history.data.HistoryRepository
import com.nyora.hasan72341.list.ui.model.MangaCompactListModel
import com.nyora.hasan72341.list.ui.model.MangaDetailedListModel
import com.nyora.hasan72341.list.ui.model.MangaGridModel
import com.nyora.hasan72341.list.ui.model.MangaListModel
import com.nyora.hasan72341.local.data.index.LocalMangaIndex
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.mihon.parsers.model.MangaTag
import com.nyora.hasan72341.tracker.domain.TrackingRepository
import com.nyora.hasan72341.tracker.domain.model.TrackingLogItem
import com.nyora.hasan72341.tracker.ui.feed.model.FeedItem
import javax.inject.Inject

@Reusable
class MangaListMapper @Inject constructor(
	@ApplicationContext context: Context,
	private val settings: AppSettings,
	private val trackingRepository: TrackingRepository,
	private val historyRepository: HistoryRepository,
	private val favouritesRepository: FavouritesRepository,
	private val localMangaIndex: LocalMangaIndex,
	private val dataRepository: MangaDataRepository,
) {


	suspend fun toListModelList(
		manga: Collection<Manga>,
		mode: ListMode,
		@Flags flags: Int = DEFAULTS,
	): List<MangaListModel> = ArrayList<MangaListModel>(manga.size).apply {
		toListModelList(
			destination = this,
			manga = manga,
			mode = mode,
			flags = flags,
		)
	}

	suspend fun toListModelList(
		destination: MutableCollection<in MangaListModel>,
		manga: Collection<Manga>,
		mode: ListMode,
		@Flags flags: Int = DEFAULTS,
	) {
		val options = getOptions(flags)
		val overrides = dataRepository.getOverrides()
		manga.mapTo(destination) {
			toListModelImpl(it, mode, options, overrides[it.id])
		}
	}

	suspend fun toListModel(
		manga: Manga,
		mode: ListMode,
		@Flags flags: Int = DEFAULTS,
	): MangaListModel = toListModelImpl(
		manga = manga,
		mode = mode,
		options = getOptions(flags),
		override = dataRepository.getOverride(manga.id),
	)

	suspend fun toFeedItem(logItem: TrackingLogItem) = FeedItem(
		id = logItem.id,
		override = dataRepository.getOverride(logItem.manga.id),
		count = logItem.chapters.size,
		manga = logItem.manga,
		isNew = logItem.isNew,
	)

	fun mapTags(tags: Collection<MangaTag>) = tags.map {
		ChipsView.ChipModel(
			tint = getTagTint(it),
			title = it.title,
			data = it,
		)
	}

	private suspend fun toCompactListModel(
		manga: Manga,
		@Options options: Int,
		override: MangaOverride?,
	) = MangaCompactListModel(
		manga = manga,
		override = override,
		subtitle = manga.tags.joinToString(", ") { it.title },
		counter = getCounter(manga.id, options),
	)

	private suspend fun toDetailedListModel(
		manga: Manga,
		@Options options: Int,
		override: MangaOverride?,
	) = MangaDetailedListModel(
		subtitle = manga.altTitles.firstOrNull(),
		manga = manga,
		override = override,
		counter = getCounter(manga.id, options),
		progress = getProgress(manga.id, options),
		isFavorite = isFavorite(manga.id, options),
		isSaved = isSaved(manga.id, options),
		tags = mapTags(manga.tags),
	)

	private suspend fun toGridModel(
		manga: Manga,
		@Options options: Int,
		override: MangaOverride?
	) = MangaGridModel(
		manga = manga,
		override = override,
		counter = getCounter(manga.id, options),
		progress = getProgress(manga.id, options),
		isFavorite = isFavorite(manga.id, options),
		isSaved = isSaved(manga.id, options),
	)

	private suspend fun toListModelImpl(
		manga: Manga,
		mode: ListMode,
		@Options options: Int,
		override: MangaOverride?,
	): MangaListModel = when (mode) {
		ListMode.LIST -> toCompactListModel(manga, options, override)
		ListMode.DETAILED_LIST -> toDetailedListModel(manga, options, override)
		ListMode.GRID -> toGridModel(manga, options, override)
	}

	private suspend fun getCounter(mangaId: String, @Options options: Int): Int {
		// Data-driven sources use non-numeric ids and aren't tracked (numeric-keyed feature).
		val numericId = mangaId.toLongOrNull() ?: return 0
		return if (settings.isTrackerEnabled) {
			trackingRepository.getNewChaptersCount(numericId)
		} else {
			0
		}
	}

	private suspend fun getProgress(mangaId: String, @Options options: Int): ReadingProgress? {
		return if (options.isBadgeEnabled(PROGRESS)) {
			historyRepository.getProgress(mangaId, settings.progressIndicatorMode)
		} else {
			null
		}
	}

	private suspend fun isFavorite(mangaId: String, @Options options: Int): Boolean {
		return options.isBadgeEnabled(FAVORITE) && favouritesRepository.isFavorite(mangaId)
	}

	private suspend fun isSaved(mangaId: String, @Options options: Int): Boolean {
		val numericId = mangaId.toLongOrNull() ?: return false
		return options.isBadgeEnabled(SAVED) && numericId in localMangaIndex
	}

	@ColorRes
	private fun getTagTint(tag: MangaTag): Int {
		return 0
	}


	private fun Int.isBadgeEnabled(@Options badge: Int) = this and badge == badge

	@Options
	@SuppressLint("WrongConstant")
	private fun getOptions(@Flags flags: Int): Int {
		var options = settings.getMangaListBadges() or PROGRESS
		options = options and flags.inv()
		return options
	}

	@IntDef(DEFAULTS, NO_SAVED, NO_PROGRESS, NO_FAVORITE, flag = true)
	@Retention(AnnotationRetention.SOURCE)
	annotation class Flags

	@IntDef(NONE, SAVED, FAVORITE, PROGRESS)
	@Retention(AnnotationRetention.SOURCE)
	private annotation class Options

	companion object {

		private const val NONE = 0
		private const val SAVED = 1
		private const val PROGRESS = 2
		private const val FAVORITE = 4

		const val DEFAULTS = NONE
		const val NO_SAVED = SAVED
		const val NO_PROGRESS = PROGRESS
		const val NO_FAVORITE = FAVORITE
	}
}
