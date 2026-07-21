package com.nyora.hasan72341.core.parser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import com.nyora.hasan72341.core.cache.MemoryContentCache
import com.nyora.hasan72341.core.model.toChronologicalChapterOrder
import com.nyora.hasan72341.core.model.toMangaSourceRef
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.mihon.parsers.model.MangaChapter
import com.nyora.hasan72341.mihon.parsers.model.MangaListFilter
import com.nyora.hasan72341.mihon.parsers.model.MangaListFilterCapabilities
import com.nyora.hasan72341.mihon.parsers.model.MangaListFilterOptions
import com.nyora.hasan72341.mihon.parsers.model.MangaPage
import com.nyora.hasan72341.mihon.parsers.model.MangaSource
import com.nyora.hasan72341.mihon.parsers.model.MangaState
import com.nyora.hasan72341.mihon.parsers.model.MangaTag
import com.nyora.hasan72341.mihon.parsers.model.SortOrder
import com.nyora.hasan72341.tachiyomi.network.await
import java.util.EnumSet

/**
 * Native ToonDex source (was Toonily.me → toondex.io). ToonDex rebuilt on the MangaBuddy
 * reader-network codebase backed by a clean JSON API (api.toondex.io); the bundled kotatsu
 * Madtheme parser no longer matches, so it returned 0 results. This app-layer repository ports
 * the API (mirrors the helper's ToonDexExtensionService) and replaces the kotatsu TOONILY_ME
 * source. Page images come from the Next.js data route because the /images API returns only a
 * 3-image anonymous preview.
 */
class ToonDexMangaRepository(
	override val source: MangaSource,
	private val okHttpClient: OkHttpClient,
	cache: MemoryContentCache,
) : CachingMangaRepository(cache) {

	private val apiUrl = "https://api.toondex.io"
	private val siteUrl = "https://toondex.io"
	private val json = Json { ignoreUnknownKeys = true }

	@Volatile
	private var cachedBuildId: String? = null

	override val sortOrders: Set<SortOrder> = EnumSet.of(SortOrder.POPULARITY, SortOrder.UPDATED)

	override var defaultSortOrder: SortOrder
		get() = SortOrder.POPULARITY
		set(value) = Unit

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(isSearchSupported = true)

	override suspend fun getFilterOptions(): MangaListFilterOptions = MangaListFilterOptions()

	// -- browse ------------------------------------------------------------
	override suspend fun getList(
		offset: Int,
		order: SortOrder?,
		filter: MangaListFilter?,
	): List<Manga> = withContext(Dispatchers.IO) {
		val page = offset / PAGE_SIZE + 1
		val query = filter?.query
		val url = "$apiUrl/titles/search".toHttpUrl().newBuilder().apply {
			if (!query.isNullOrBlank()) {
				addQueryParameter("q", query)
			} else {
				addQueryParameter("sort", if ((order ?: defaultSortOrder) == SortOrder.UPDATED) "latest" else "popular")
			}
			addQueryParameter("page", page.toString())
		}.build().toString()
		getJson<Envelope<SearchData>>(url).data.items.map { it.toManga() }
	}

	// -- details + chapters ------------------------------------------------
	override suspend fun getDetailsImpl(manga: Manga): Manga = withContext(Dispatchers.IO) {
		val id = manga.url.trim('/')
		val title = getJson<Envelope<TitleData>>("$apiUrl/titles/$id").data.title

		val chapters = ArrayList<MangaChapter>()
		var page = 1
		var hasNext: Boolean
		do {
			val url = "$apiUrl/titles/$id/chapters".toHttpUrl().newBuilder()
				.addQueryParameter("page", page.toString())
				.build().toString()
			val data = getJson<Envelope<ChaptersData>>(url).data
			data.chapters.forEach { chapters.add(it.toChapter(id)) }
			hasNext = data.pagination?.hasNext == true
			page++
		} while (hasNext && page <= 200)

		title.toManga(manga).copy(chapters = chapters.toChronologicalChapterOrder())
	}

	// -- pages -------------------------------------------------------------
	override suspend fun getPagesImpl(chapter: MangaChapter): List<MangaPage> = withContext(Dispatchers.IO) {
		val path = chapter.url.trim('/')
		suspend fun fetch(bid: String): List<String> =
			getJson<NextData>("$siteUrl/_next/data/$bid/$path.json").pageProps.initialChapter.images
		val images = runCatching { fetch(buildId()) }.getOrElse { fetch(buildId(force = true)) }
		images.mapIndexed { index, u ->
			MangaPage(
				url = u,
				headers = mapOf("Referer" to "$siteUrl/"),
				id = "${chapter.url}|page|$index",
				index = index,
				source = source,
			)
		}
	}

	override suspend fun getPageUrl(page: MangaPage): String = page.url

	override suspend fun getRelatedMangaImpl(seed: Manga): List<Manga> = emptyList()

	// -- http --------------------------------------------------------------
	private suspend inline fun <reified T> getJson(url: String): T {
		val request = Request.Builder()
			.url(url)
			.header("Referer", "$siteUrl/")
			.header("Accept", "application/json")
			.build()
		val body = okHttpClient.newCall(request).await().use { it.body?.string().orEmpty() }
		return json.decodeFromString<T>(body)
	}

	// The site's Next.js build id (rotates per deploy); needed for the _next/data page route.
	private suspend fun buildId(force: Boolean = false): String {
		cachedBuildId?.takeUnless { force }?.let { return it }
		val request = Request.Builder().url("$siteUrl/").header("Referer", "$siteUrl/").build()
		val html = okHttpClient.newCall(request).await().use { it.body?.string().orEmpty() }
		val id = Regex("\"buildId\":\"([^\"]+)\"").find(html)?.groupValues?.get(1)
			?: error("ToonDex buildId not found")
		cachedBuildId = id
		return id
	}

	private fun TitleItem.toManga(): Manga = Manga(
		id = "${source.name}|$id",
		title = name,
		url = id,
		publicUrl = siteUrl + url,
		coverUrl = cover.orEmpty(),
		largeCoverUrl = cover,
		source = source.name.toMangaSourceRef(),
	)

	private fun TitleFull.toManga(seed: Manga): Manga {
		val tags = buildList {
			genres?.forEach { add(it.name) }
			themes?.forEach { add(it.name) }
		}.distinct().map { MangaTag(key = it, title = it) }
		return Manga(
			id = "${source.name}|$id",
			title = name,
			url = id,
			publicUrl = siteUrl + (url ?: "/$slug"),
			coverUrl = cover.orEmpty().ifEmpty { seed.coverUrl },
			largeCoverUrl = cover ?: seed.largeCoverUrl,
			authors = authors?.map { it.name }.orEmpty(),
			description = summary?.let { Jsoup.parse(it).text() }.orEmpty(),
			tags = tags,
			state = when (status?.lowercase()) {
				"ongoing", "releasing" -> MangaState.ONGOING
				"completed", "finished" -> MangaState.FINISHED
				"hiatus", "on hold" -> MangaState.PAUSED
				"cancelled", "dropped" -> MangaState.ABANDONED
				else -> null
			},
			source = source.name.toMangaSourceRef(),
		)
	}

	private fun ChapterItem.toChapter(mangaId: String): MangaChapter {
		val path = (url ?: "$mangaId/$id").trim('/')
		return MangaChapter(
			id = "${source.name}|chapter|$path",
			title = name ?: number?.let { "Chapter ${it.toString().removeSuffix(".0")}" } ?: "Chapter",
			number = number ?: 0f,
			url = path,
			uploadDate = updatedAt?.let { runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull() } ?: 0L,
		)
	}

	private companion object {
		const val PAGE_SIZE = 20
	}
}

// -- API DTOs ---------------------------------------------------------------
@Serializable
private class Envelope<T>(val data: T)

@Serializable
private class Pagination(@SerialName("has_next") val hasNext: Boolean = false)

@Serializable
private class SearchData(val items: List<TitleItem> = emptyList(), val pagination: Pagination? = null)

@Serializable
private class TitleItem(
	val id: String,
	val url: String,
	val name: String,
	val slug: String? = null,
	val cover: String? = null,
	val status: String? = null,
)

@Serializable
private class TitleData(val title: TitleFull)

@Serializable
private class TitleFull(
	val id: String,
	val url: String? = null,
	val name: String,
	val slug: String? = null,
	val cover: String? = null,
	val summary: String? = null,
	val status: String? = null,
	val authors: List<NamedRef>? = null,
	val genres: List<NamedRef>? = null,
	val themes: List<NamedRef>? = null,
)

@Serializable
private class NamedRef(val name: String)

@Serializable
private class ChaptersData(val chapters: List<ChapterItem> = emptyList(), val pagination: Pagination? = null)

@Serializable
private class ChapterItem(
	val id: String,
	val url: String? = null,
	val name: String? = null,
	val number: Float? = null,
	@SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
private class NextData(val pageProps: NextPageProps)

@Serializable
private class NextPageProps(val initialChapter: NextChapter)

@Serializable
private class NextChapter(val images: List<String> = emptyList())
