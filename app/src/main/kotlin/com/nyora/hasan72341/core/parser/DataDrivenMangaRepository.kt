package com.nyora.hasan72341.core.parser

import app.nyora.data.engine.EngineContext
import app.nyora.data.engine.EngineRegistry
import app.nyora.data.engine.SourceEngine
import com.nyora.hasan72341.core.model.DataDrivenMangaSource
import com.nyora.hasan72341.core.parser.datadriven.toSourceDef
import com.nyora.hasan72341.mihon.parsers.model.ContentRating
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.mihon.parsers.model.MangaChapter
import com.nyora.hasan72341.mihon.parsers.model.MangaListFilter
import com.nyora.hasan72341.mihon.parsers.model.MangaListFilterCapabilities
import com.nyora.hasan72341.mihon.parsers.model.MangaListFilterOptions
import com.nyora.hasan72341.mihon.parsers.model.MangaPage
import com.nyora.hasan72341.mihon.parsers.model.MangaSource
import com.nyora.hasan72341.mihon.parsers.model.MangaSourceRef
import com.nyora.hasan72341.mihon.parsers.model.MangaState
import com.nyora.hasan72341.mihon.parsers.model.MangaTag
import com.nyora.hasan72341.mihon.parsers.model.SortOrder
import app.nyora.core.model.Manga as DdManga
import app.nyora.core.model.MangaChapter as DdChapter
import app.nyora.core.model.MangaPage as DdPage
import app.nyora.core.model.ContentRating as DdRating
import app.nyora.core.model.MangaState as DdState
import app.nyora.core.model.SortOrder as DdSort

/**
 * A [MangaRepository] backed by a bundled generic [SourceEngine] instead of a compiled per-source
 * parser. The engine is constructed lazily from the [DataDrivenMangaSource]'s data (engine id +
 * domain + config) via the [EngineRegistry], and every call delegates to it with the results mapped
 * between the data-driven model (String ids, List collections) and the app's model. This is the path
 * that lets sources be fetched at runtime rather than baked into the APK.
 */
class DataDrivenMangaRepository(
    private val ddSource: DataDrivenMangaSource,
    private val context: EngineContext,
) : MangaRepository {

    override val source: MangaSource get() = ddSource

    private val sourceRef: MangaSourceRef = MangaSourceRef.Parser(ddSource.name)

    private val engine: SourceEngine by lazy {
        EngineRegistry.create(ddSource.engineKey, ddSource.toSourceDef(), context)
    }

    private val pageSize: Int = (ddSource.config["pageSize"] as? Number)?.toInt() ?: 20

    override val sortOrders: Set<SortOrder>
        get() = engine.availableSortOrders.mapNotNull { it.toFork() }.toSet()
            .ifEmpty { setOf(SortOrder.POPULARITY) }

    override var defaultSortOrder: SortOrder = SortOrder.POPULARITY

    override val filterCapabilities: MangaListFilterCapabilities = MangaListFilterCapabilities()

    override suspend fun getList(offset: Int, order: SortOrder?, filter: MangaListFilter?): List<Manga> {
        val page = if (pageSize > 0) offset / pageSize else offset
        val query = filter?.query?.takeIf { it.isNotBlank() }
        val ddList = when {
            query != null -> engine.search(page, query)
            order == SortOrder.UPDATED || order == SortOrder.NEWEST -> engine.getLatest(page)
            else -> engine.getPopular(page)
        }
        return ddList.map { it.toFork() }
    }

    override suspend fun getDetails(manga: Manga): Manga {
        val stub = DdManga(id = manga.id, title = manga.title, url = manga.url)
        return engine.getDetails(stub).toFork()
    }

    override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
        val ddChapter = DdChapter(
            id = chapter.id,
            url = chapter.url,
            number = chapter.number,
            volume = chapter.volume,
        )
        return engine.getPageList(ddChapter).map { it.toFork() }
    }

    override suspend fun getPageUrl(page: MangaPage): String {
        val ddPage = DdPage(url = page.url, id = page.id.ifEmpty { page.url })
        return engine.getPageImageUrl(ddPage)
    }

    override suspend fun getFilterOptions(): MangaListFilterOptions = MangaListFilterOptions()

    override suspend fun getRelated(seed: Manga): List<Manga> = emptyList()

    // ---- model mapping: data-driven -> app ----

    private fun DdManga.toFork(): Manga = Manga(
        id = id,
        title = title,
        altTitles = altTitles.toList(),
        url = url,
        publicUrl = publicUrl,
        rating = rating,
        isNsfw = isNsfw,
        contentRating = contentRating?.toFork(),
        coverUrl = coverUrl.orEmpty(),
        largeCoverUrl = largeCoverUrl,
        state = state?.toFork(),
        authors = authors.toList(),
        source = sourceRef,
        description = description.orEmpty(),
        tags = tags.map { MangaTag(key = it.key, title = it.title) },
        chapters = chapters?.map { it.toFork() } ?: emptyList(),
    )

    private fun DdChapter.toFork(): MangaChapter = MangaChapter(
        id = id,
        title = title.orEmpty(),
        number = number,
        volume = volume,
        url = url,
        scanlator = scanlator,
        uploadDate = uploadDate,
        branch = branch,
    )

    private fun DdPage.toFork(): MangaPage = MangaPage(url = url, id = id, preview = preview)

    private fun DdState.toFork(): MangaState? = runCatching { MangaState.valueOf(name) }.getOrNull()
    private fun DdRating.toFork(): ContentRating? = runCatching { ContentRating.valueOf(name) }.getOrNull()
    private fun DdSort.toFork(): SortOrder? = runCatching { SortOrder.valueOf(name) }.getOrNull()
}
