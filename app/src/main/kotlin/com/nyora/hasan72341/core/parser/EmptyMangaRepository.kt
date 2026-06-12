package com.nyora.hasan72341.core.parser

import com.nyora.hasan72341.core.exceptions.UnsupportedSourceException
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.mihon.parsers.model.MangaChapter
import com.nyora.hasan72341.mihon.parsers.model.MangaListFilter
import com.nyora.hasan72341.mihon.parsers.model.MangaListFilterCapabilities
import com.nyora.hasan72341.mihon.parsers.model.MangaListFilterOptions
import com.nyora.hasan72341.mihon.parsers.model.MangaPage
import com.nyora.hasan72341.mihon.parsers.model.MangaSource
import com.nyora.hasan72341.mihon.parsers.model.SortOrder
import java.util.EnumSet

open class EmptyMangaRepository(override val source: MangaSource) : MangaRepository {

	override val sortOrders: Set<SortOrder>
		get() = EnumSet.allOf(SortOrder::class.java)

	override var defaultSortOrder: SortOrder
		get() = SortOrder.NEWEST
		set(value) = Unit

	override val filterCapabilities: MangaListFilterCapabilities
		@OptIn(org.koitharu.kotatsu.parsers.InternalParsersApi::class)
		get() = MangaListFilterCapabilities()

	override suspend fun getList(offset: Int, order: SortOrder?, filter: MangaListFilter?): List<Manga> = stub()

	override suspend fun getDetails(manga: Manga): Manga = stub(manga)

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> = stub()

	override suspend fun getPageUrl(page: MangaPage): String = stub()

	override suspend fun getFilterOptions(): MangaListFilterOptions = stub()

	override suspend fun getRelated(seed: Manga): List<Manga> = stub(seed)

	private fun stub(manga: Manga? = null): Nothing {
		throw UnsupportedSourceException("This manga source is not supported: ${source.name}", manga, source)
	}
}
