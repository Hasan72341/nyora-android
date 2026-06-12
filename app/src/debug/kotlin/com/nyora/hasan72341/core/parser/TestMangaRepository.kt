package com.nyora.hasan72341.core.parser

import com.nyora.hasan72341.core.cache.MemoryContentCache
import com.nyora.hasan72341.core.model.TestMangaSource
import com.nyora.hasan72341.mihon.parsers.MangaLoaderContext
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.mihon.parsers.model.MangaChapter
import com.nyora.hasan72341.mihon.parsers.model.MangaListFilter
import com.nyora.hasan72341.mihon.parsers.model.MangaListFilterCapabilities
import com.nyora.hasan72341.mihon.parsers.model.MangaListFilterOptions
import com.nyora.hasan72341.mihon.parsers.model.MangaPage
import com.nyora.hasan72341.mihon.parsers.model.SortOrder
import java.util.EnumSet

/*
 This class is for parser development and testing purposes
 You can open it in the app via Settings -> Debug
 */
@OptIn(org.koitharu.kotatsu.parsers.InternalParsersApi::class)
class TestMangaRepository(
	@Suppress("unused") private val loaderContext: MangaLoaderContext,
	cache: MemoryContentCache
) : CachingMangaRepository(cache) {

	override val source = TestMangaSource

	override val sortOrders: Set<SortOrder> = EnumSet.allOf(SortOrder::class.java)

	override var defaultSortOrder: SortOrder
		get() = sortOrders.first()
		set(value) = Unit

	override val filterCapabilities = MangaListFilterCapabilities()

	override suspend fun getFilterOptions() = MangaListFilterOptions()

	override suspend fun getList(
		offset: Int,
		order: SortOrder?,
		filter: MangaListFilter?
	): List<Manga> = TODO("Get manga list by filter")

	override suspend fun getDetailsImpl(
		manga: Manga
	): Manga = TODO("Fetch manga details")

	override suspend fun getPagesImpl(
		chapter: MangaChapter
	): List<MangaPage> = TODO("Get pages for specific chapter")

	override suspend fun getPageUrl(
		page: MangaPage
	): String = TODO("Return direct url of page image or page.url if it is already a direct url")

	override suspend fun getRelatedMangaImpl(
		seed: Manga
	): List<Manga> = TODO("Get list of related manga. This method is optional and parser library has a default implementation")
}
