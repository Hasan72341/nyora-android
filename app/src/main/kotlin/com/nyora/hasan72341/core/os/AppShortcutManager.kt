package com.nyora.hasan72341.core.os

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ShortcutManager
import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.room.InvalidationTracker
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.size.Scale
import coil3.size.Size
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.LocalizedAppContext
import com.nyora.hasan72341.core.db.TABLE_HISTORY
import com.nyora.hasan72341.core.model.getTitle
import com.nyora.hasan72341.core.model.toMangaSource
import com.nyora.hasan72341.core.nav.AppRouter
import com.nyora.hasan72341.core.nav.ReaderIntent
import com.nyora.hasan72341.core.parser.MangaDataRepository
import com.nyora.hasan72341.core.parser.favicon.faviconUri
import com.nyora.hasan72341.core.prefs.AppSettings
import com.nyora.hasan72341.core.ui.image.ThumbnailTransformation
import com.nyora.hasan72341.core.util.ext.getDrawableOrThrow
import com.nyora.hasan72341.core.util.ext.mangaSourceExtra
import com.nyora.hasan72341.mihon.parsers.util.mapToSet
import com.nyora.hasan72341.core.util.ext.printStackTraceDebug
import com.nyora.hasan72341.core.util.ext.processLifecycleScope
import com.nyora.hasan72341.history.data.HistoryRepository
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.mihon.parsers.model.MangaSource
import com.nyora.hasan72341.mihon.parsers.util.ifNullOrEmpty
import com.nyora.hasan72341.mihon.parsers.util.mapNotNullToSet
import com.nyora.hasan72341.mihon.parsers.util.runCatchingCancellable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppShortcutManager @Inject constructor(
	@LocalizedAppContext private val context: Context,
	private val coil: ImageLoader,
	private val historyRepository: HistoryRepository,
	private val mangaRepository: MangaDataRepository,
	private val settings: AppSettings,
) : InvalidationTracker.Observer(TABLE_HISTORY), SharedPreferences.OnSharedPreferenceChangeListener {

	private val iconSize by lazy {
		Size(ShortcutManagerCompat.getIconMaxWidth(context), ShortcutManagerCompat.getIconMaxHeight(context))
	}
	private var shortcutsUpdateJob: Job? = null

	init {
		settings.subscribe(this)
	}

	override fun onInvalidated(tables: Set<String>) {
		if (!settings.isDynamicShortcutsEnabled) {
			return
		}
		val prevJob = shortcutsUpdateJob
		shortcutsUpdateJob = processLifecycleScope.launch(Dispatchers.IO) {
			prevJob?.join()
			updateShortcutsImpl()
		}
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
		if (key == AppSettings.KEY_SHORTCUTS) {
			if (settings.isDynamicShortcutsEnabled) {
				onInvalidated(emptySet())
			} else {
				clearShortcuts()
			}
		}
	}

	suspend fun requestPinShortcut(manga: Manga): Boolean = try {
		ShortcutManagerCompat.requestPinShortcut(context, buildShortcutInfo(manga), null)
	} catch (e: IllegalStateException) {
		e.printStackTraceDebug("AppShortcutManager::requestPinShortcut")
		false
	}

	suspend fun requestPinShortcut(source: MangaSource): Boolean = try {
		ShortcutManagerCompat.requestPinShortcut(context, buildShortcutInfo(source), null)
	} catch (e: IllegalStateException) {
		e.printStackTraceDebug("AppShortcutManager::requestPinShortcut")
		false
	}

	fun getMangaShortcuts(): Set<String> {
		val shortcuts = ShortcutManagerCompat.getShortcuts(
			context,
			ShortcutManagerCompat.FLAG_MATCH_CACHED or ShortcutManagerCompat.FLAG_MATCH_PINNED or ShortcutManagerCompat.FLAG_MATCH_DYNAMIC,
		)
		return shortcuts.mapToSet { it.id }
	}

	@VisibleForTesting
	suspend fun await(): Boolean {
		return shortcutsUpdateJob?.join() != null
	}

	fun notifyMangaOpened(mangaId: String) {
		ShortcutManagerCompat.reportShortcutUsed(context, mangaId)
	}

	fun isDynamicShortcutsAvailable(): Boolean {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 &&
			context.getSystemService(ShortcutManager::class.java).maxShortcutCountPerActivity > 0
	}

	private suspend fun updateShortcutsImpl() = runCatchingCancellable {
		val maxShortcuts = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context).coerceAtLeast(5)
		val shortcuts = historyRepository.getList(0, maxShortcuts)
			.filter { x -> x.title.isNotEmpty() }
			.map { buildShortcutInfo(it) }
		ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
	}.onFailure {
		it.printStackTraceDebug("AppShortcutManager::updateShortcutsImpl")
	}

	private fun clearShortcuts() {
		try {
			ShortcutManagerCompat.removeAllDynamicShortcuts(context)
		} catch (_: IllegalStateException) {
		}
	}

	private suspend fun buildShortcutInfo(manga: Manga): ShortcutInfoCompat = withContext(Dispatchers.IO) {
		val icon = runCatchingCancellable {
			coil.execute(
				ImageRequest.Builder(context)
					.data(manga.coverUrl)
					.size(iconSize)
					.mangaSourceExtra(manga.source.toMangaSource())
					.scale(Scale.FILL)
					.transformations(ThumbnailTransformation())
					.build(),
			).getDrawableOrThrow().toBitmap()
		}.fold(
			onSuccess = { IconCompat.createWithAdaptiveBitmap(it) },
			onFailure = { IconCompat.createWithResource(context, R.drawable.ic_shortcut_default) },
		)
		mangaRepository.storeManga(manga, replaceExisting = true)
		val title = manga.title.ifEmpty {
			manga.altTitles.firstOrNull()
		}.ifNullOrEmpty {
			context.getString(R.string.unknown)
		}
			ShortcutInfoCompat.Builder(context, manga.id)
			.setShortLabel(title)
			.setLongLabel(title)
			.setIcon(icon)
			.setLongLived(true)
			.setIntent(
				ReaderIntent.Builder(context)
					.mangaId(manga.id)
					.build()
					.intent,
			).build()
	}

	private suspend fun buildShortcutInfo(source: MangaSource): ShortcutInfoCompat = withContext(Dispatchers.IO) {
		val icon = runCatchingCancellable {
			coil.execute(
				ImageRequest.Builder(context)
					.data(source.faviconUri())
					.mangaSourceExtra(source)
					.size(iconSize)
					.scale(Scale.FIT)
					.build(),
			).getDrawableOrThrow().toBitmap()
		}.fold(
			onSuccess = { IconCompat.createWithAdaptiveBitmap(it) },
			onFailure = { IconCompat.createWithResource(context, R.drawable.ic_shortcut_default) },
		)
		val title = source.getTitle(context)
		ShortcutInfoCompat.Builder(context, source.name)
			.setShortLabel(title)
			.setLongLabel(title)
			.setIcon(icon)
			.setLongLived(true)
			.setIntent(AppRouter.listIntent(context, source, null, null))
			.build()
	}
}
