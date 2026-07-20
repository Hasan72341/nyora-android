package com.nyora.hasan72341.core.parser

import android.content.Context
import androidx.annotation.AnyThread
import androidx.collection.ArrayMap
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import com.nyora.hasan72341.core.cache.MemoryContentCache
import com.nyora.hasan72341.core.network.MangaHttpClient
import com.nyora.hasan72341.core.model.DataDrivenMangaSource
import com.nyora.hasan72341.core.model.LocalMangaSource
import com.nyora.hasan72341.core.model.MangaSourceInfo
import com.nyora.hasan72341.core.parser.datadriven.AndroidEngineContext
import com.nyora.hasan72341.core.model.TestMangaSource
import com.nyora.hasan72341.core.model.UnknownMangaSource
import com.nyora.hasan72341.local.data.LocalMangaRepository
import com.nyora.hasan72341.mihon.parsers.MangaLoaderContext
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.mihon.parsers.model.MangaChapter
import com.nyora.hasan72341.mihon.parsers.model.MangaListFilter
import com.nyora.hasan72341.mihon.parsers.model.MangaListFilterCapabilities
import com.nyora.hasan72341.mihon.parsers.model.MangaListFilterOptions
import com.nyora.hasan72341.mihon.parsers.model.MangaPage
import com.nyora.hasan72341.mihon.parsers.model.MangaParserSource
import com.nyora.hasan72341.mihon.parsers.model.MangaSource
import com.nyora.hasan72341.mihon.parsers.model.SortOrder
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

interface MangaRepository {

	val source: MangaSource

	val sortOrders: Set<SortOrder>

	var defaultSortOrder: SortOrder

	val filterCapabilities: MangaListFilterCapabilities

	suspend fun getList(offset: Int, order: SortOrder?, filter: MangaListFilter?): List<Manga>

	suspend fun getDetails(manga: Manga): Manga

	suspend fun getPages(chapter: MangaChapter): List<MangaPage>

	suspend fun getPageUrl(page: MangaPage): String

	suspend fun getFilterOptions(): MangaListFilterOptions

	suspend fun getRelated(seed: Manga): List<Manga>

	suspend fun find(manga: Manga): Manga? {
		val list = getList(0, SortOrder.RELEVANCE, MangaListFilter(query = manga.title))
		return list.find { x -> x.id == manga.id }
	}

	@Singleton
	class Factory @Inject constructor(
		@ApplicationContext private val context: Context,
		private val localMangaRepository: LocalMangaRepository,
		private val loaderContext: MangaLoaderContext,
		private val contentCache: MemoryContentCache,
		private val mirrorSwitcher: MirrorSwitcher,
		@MangaHttpClient private val okHttpClient: OkHttpClient,
	) {

		private val cache = ArrayMap<MangaSource, WeakReference<MangaRepository>>()

		@AnyThread
		fun create(source: MangaSource): MangaRepository {
			when (source) {
				is MangaSourceInfo -> return create(source.mangaSource)
				LocalMangaSource -> return localMangaRepository
				UnknownMangaSource -> return EmptyMangaRepository(source)
			}
			cache[source]?.get()?.let { return it }
			return synchronized(cache) {
				cache[source]?.get()?.let { return it }
				val repository = createRepository(source)
				if (repository != null) {
					cache[source] = WeakReference(repository)
					repository
				} else {
					EmptyMangaRepository(source)
				}
			}
		}

		private fun createRepository(source: MangaSource): MangaRepository? = when (source) {
			TestMangaSource -> TestMangaRepository(
				loaderContext = loaderContext,
				cache = contentCache,
			)

			// MangaFire moved to a new JSON API the bundled kotatsu parser can't read; back the
			// MANGAFIRE_* enum entries with the native repository instead. Must precede the
			// generic MangaParserSource branch below.
			is MangaParserSource if source.name.startsWith("MANGAFIRE") -> MangaFireMangaRepository(
				source = source,
				okHttpClient = okHttpClient,
				cache = contentCache,
			)

			// Toonily.me → toondex.io (MangaBuddy JSON API); kotatsu Madtheme parser no longer matches.
			is MangaParserSource if source.name == "TOONILY_ME" -> ToonDexMangaRepository(
				source = source,
				okHttpClient = okHttpClient,
				cache = contentCache,
			)

			is MangaParserSource -> ParserMangaRepository(
				parser = loaderContext.newParserInstance(source),
				mirrorSwitcher = mirrorSwitcher,
				cache = contentCache,
			)

			// Data-driven source: rendered at runtime by a bundled generic engine from
			// fetched SourceDef data, with no per-source parser in the APK.
			is DataDrivenMangaSource -> DataDrivenMangaRepository(
				ddSource = source,
				context = AndroidEngineContext(okHttpClient),
			)

			else -> null
		}
	}
}
