package com.nyora.hasan72341.details.service

import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import com.nyora.hasan72341.core.cache.MemoryContentCache
import com.nyora.hasan72341.core.model.LocalMangaSource
import com.nyora.hasan72341.core.model.findChapterById
import com.nyora.hasan72341.core.model.isLocal
import com.nyora.hasan72341.core.model.toMangaSource
import com.nyora.hasan72341.core.model.parcelable.ParcelableChapter
import com.nyora.hasan72341.core.model.parcelable.ParcelableManga
import com.nyora.hasan72341.core.parser.MangaRepository
import com.nyora.hasan72341.core.ui.CoroutineIntentService
import com.nyora.hasan72341.core.util.ext.getParcelableExtraCompat
import com.nyora.hasan72341.core.util.ext.isPowerSaveMode
import com.nyora.hasan72341.core.util.ext.printStackTraceDebug
import com.nyora.hasan72341.history.data.HistoryRepository
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.mihon.parsers.model.MangaChapter
import com.nyora.hasan72341.mihon.parsers.model.MangaSource
import com.nyora.hasan72341.mihon.parsers.util.runCatchingCancellable
import javax.inject.Inject

@AndroidEntryPoint
class MangaPrefetchService : CoroutineIntentService() {

	@Inject
	lateinit var mangaRepositoryFactory: MangaRepository.Factory

	@Inject
	lateinit var cache: MemoryContentCache

	@Inject
	lateinit var historyRepository: HistoryRepository

	override suspend fun IntentJobContext.processIntent(intent: Intent) {
		when (intent.action) {
			ACTION_PREFETCH_DETAILS -> prefetchDetails(
				manga = intent.getParcelableExtraCompat<ParcelableManga>(EXTRA_MANGA)?.manga
					?: return,
			)

			ACTION_PREFETCH_PAGES -> prefetchPages(
				chapter = intent.getParcelableExtraCompat<ParcelableChapter>(EXTRA_CHAPTER)?.chapter
					?: return,
			)

			ACTION_PREFETCH_LAST -> prefetchLast()
		}
	}

	override fun IntentJobContext.onError(error: Throwable) = Unit

	private suspend fun prefetchDetails(manga: Manga) {
		val source = mangaRepositoryFactory.create(manga.source.toMangaSource())
		runCatchingCancellable { source.getDetails(manga) }
	}

	private suspend fun prefetchPages(chapter: MangaChapter) {
		return
	}

	private suspend fun prefetchLast() {
		val last = historyRepository.getLastOrNull() ?: return
		if (last.isLocal) return
		val repo = mangaRepositoryFactory.create(last.source.toMangaSource())
		val details = runCatchingCancellable { repo.getDetails(last) }.getOrNull() ?: return
		val chapters = details.chapters
		if (chapters.isNullOrEmpty()) {
			return
		}
		val history = historyRepository.getOne(last)
		val chapter = if (history == null) {
			chapters.firstOrNull()
		} else {
			details.findChapterById(history.chapterId.toString()) ?: chapters.firstOrNull()
		} ?: return
		runCatchingCancellable { repo.getPages(chapter) }
	}

	companion object {

		private const val EXTRA_MANGA = "manga"
		private const val EXTRA_CHAPTER = "manga"
		private const val ACTION_PREFETCH_DETAILS = "details"
		private const val ACTION_PREFETCH_PAGES = "pages"
		private const val ACTION_PREFETCH_LAST = "last"

		fun prefetchDetails(context: Context, manga: Manga) {
			if (!isPrefetchAvailable(context, manga.source.toMangaSource())) return
			val intent = Intent(context, MangaPrefetchService::class.java)
			intent.action = ACTION_PREFETCH_DETAILS
			intent.putExtra(EXTRA_MANGA, ParcelableManga(manga))
			tryStart(context, intent)
		}

		fun prefetchPages(context: Context, chapter: MangaChapter) {
			if (!isPrefetchAvailable(context, null)) return
			val intent = Intent(context, MangaPrefetchService::class.java)
			intent.action = ACTION_PREFETCH_PAGES
			intent.putExtra(EXTRA_CHAPTER, ParcelableChapter(chapter))
			tryStart(context, intent)
		}

		fun prefetchLast(context: Context) {
			if (!isPrefetchAvailable(context, null)) return
			val intent = Intent(context, MangaPrefetchService::class.java)
			intent.action = ACTION_PREFETCH_LAST
			tryStart(context, intent)
		}

		private fun isPrefetchAvailable(context: Context, source: MangaSource?): Boolean {
			if (source == LocalMangaSource || context.isPowerSaveMode()) {
				return false
			}
			val entryPoint = EntryPointAccessors.fromApplication(
				context,
				PrefetchCompanionEntryPoint::class.java,
			)
			return entryPoint.settings.isContentPrefetchEnabled
		}

		private fun tryStart(context: Context, intent: Intent) {
			try {
				context.startService(intent)
			} catch (e: IllegalStateException) {
				// probably app is in background
				e.printStackTraceDebug("MangaPrefetchService::tryStart")
			}
		}
	}
}
