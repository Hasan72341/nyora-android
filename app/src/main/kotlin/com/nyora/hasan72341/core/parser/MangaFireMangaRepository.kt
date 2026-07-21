package com.nyora.hasan72341.core.parser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import com.nyora.hasan72341.core.model.DataDrivenMangaSource
import com.nyora.hasan72341.mihon.parsers.model.MangaSource
import com.nyora.hasan72341.mihon.parsers.model.MangaState
import com.nyora.hasan72341.mihon.parsers.model.MangaTag
import com.nyora.hasan72341.mihon.parsers.model.SortOrder
import com.nyora.hasan72341.tachiyomi.network.await
import java.util.EnumSet

/**
 * Native MangaFire source. MangaFire relaunched on a new backend backed by a clean JSON API
 * (`/api/titles`) — no vrf token, no `/filter` HTML, no image scrambling — which the bundled
 * kotatsu-parsers MangaFire no longer matches, so it returns 0 results. This app-layer
 * repository ports the verified new API (see the nyora-mihon-extension MangaFireSource) and
 * replaces the kotatsu `MANGAFIRE_*` sources without touching the (JAR-baked) parser lib.
 *
 * One repository instance backs each MangaFire language enum entry; the per-source language is
 * derived from the enum suffix and applied to the chapter list request only. Browsing is a
 * single shared catalog (language is intentionally ignored there — matches MangaFire itself).
 */
class MangaFireMangaRepository(
	override val source: MangaSource,
	private val okHttpClient: OkHttpClient,
	cache: MemoryContentCache,
) : CachingMangaRepository(cache) {

	private val baseUrl = "https://mangafire.to"

	private val json = Json { ignoreUnknownKeys = true }

	// MangaFire's own language code. Data-driven sources carry it verbatim in their config
	// (en, es, es-la, fr, ja, pt-br); the legacy native enum path derives it from the name suffix.
	private val langCode: String = when (val s = source) {
		is DataDrivenMangaSource -> (s.config["language"] as? String)?.lowercase() ?: "en"
		else -> when (s.name.removePrefix("MANGAFIRE_")) {
			"ES" -> "es"
			"ESLA" -> "es-la"
			"FR" -> "fr"
			"JA" -> "ja"
			"PT" -> "pt"
			"PTBR" -> "pt-br"
			else -> "en" // EN and any unexpected suffix
		}
	}

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
		val url = "$baseUrl/api/titles".toHttpUrl().newBuilder().apply {
			when {
				!query.isNullOrBlank() -> addQueryParameter("keyword", query)
				(order ?: defaultSortOrder) == SortOrder.UPDATED ->
					addQueryParameter("order[chapter_updated_at]", "desc")
				else -> addQueryParameter("order[views_30d]", "desc")
			}
			addQueryParameter("page", page.toString())
			addQueryParameter("limit", PAGE_SIZE.toString())
		}.build().toString()
		getJson<ApiResponse<MangaDto>>(url).items.map { it.toManga() }
	}

	// -- details + chapters ------------------------------------------------
	override suspend fun getDetailsImpl(manga: Manga): Manga = withContext(Dispatchers.IO) {
		val hid = manga.url.hid()
		val details = getJson<MangaDetailsResponse>("$baseUrl/api/titles/$hid").data

		val chapters = ArrayList<MangaChapter>()
		var page = 1
		var lastPage: Int
		do {
			val url = "$baseUrl/api/titles/$hid/chapters".toHttpUrl().newBuilder()
				.addQueryParameter("language", langCode)
				.addQueryParameter("sort", "number")
				.addQueryParameter("order", "desc")
				.addQueryParameter("page", page.toString())
				.addQueryParameter("limit", "200")
				.build().toString()
			val response = getJson<ApiResponse<ChapterDto>>(url)
			response.items.forEach { chapters.add(it.toChapter(manga.url)) }
			lastPage = response.meta?.lastPage ?: 1
			page++
		} while (page <= lastPage)

		details.toManga(manga).copy(chapters = chapters.toChronologicalChapterOrder())
	}

	// -- pages -------------------------------------------------------------
	override suspend fun getPagesImpl(chapter: MangaChapter): List<MangaPage> = withContext(Dispatchers.IO) {
		val segments = (baseUrl + chapter.url).toHttpUrl().pathSegments
		val last = segments.last()
		val url = if (segments.contains("volume")) {
			"$baseUrl/api/volumes/$last"
		} else {
			"$baseUrl/api/chapters/${last.substringBefore("-")}"
		}
		getJson<PagesResponse>(url).data.pages.mapIndexed { index, p ->
			MangaPage(
				url = p.url,
				headers = mapOf("Referer" to "$baseUrl/"),
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
			.header("Referer", "$baseUrl/")
			.header("Accept", "application/json")
			.build()
		val body = okHttpClient.newCall(request).await().use { it.body?.string().orEmpty() }
		return json.decodeFromString<T>(body)
	}

	// MangaFire manga urls look like /title/<hid>-<slug>; the hid is the leading token.
	private fun String.hid(): String {
		val last = removeSuffix("/").substringAfterLast('/')
		return when {
			last.contains('.') -> last.substringAfterLast('.')
			last.contains('-') -> last.substringBefore('-')
			else -> last
		}
	}

	private fun MangaDto.toManga(): Manga {
		val mangaUrl = "/title/$hid${slug?.let { "-$it" }.orEmpty()}"
		return Manga(
			id = "${source.name}|$mangaUrl",
			title = title,
			url = mangaUrl,
			publicUrl = baseUrl + mangaUrl,
			coverUrl = (poster?.large ?: poster?.medium ?: poster?.small).orEmpty(),
			largeCoverUrl = poster?.large,
			source = source.name.toMangaSourceRef(),
		)
	}

	private fun MangaDetailsDto.toManga(seed: Manga): Manga {
		val mangaUrl = "/title/$hid${slug?.let { "-$it" }.orEmpty()}"
		val authorNames = buildList {
			authors?.forEach { add(it.title) }
			artists?.forEach { add(it.title) }
		}.distinct()
		val tags = buildList {
			type?.replaceFirstChar { it.uppercaseChar() }?.let { add(it) }
			genres?.forEach { add(it.title) }
			themes?.forEach { add(it.title) }
		}.map { MangaTag(key = it, title = it) }
		return Manga(
			id = "${source.name}|$mangaUrl",
			title = title,
			url = mangaUrl,
			publicUrl = baseUrl + mangaUrl,
			coverUrl = (poster?.large ?: poster?.medium ?: poster?.small)
				.orEmpty().ifEmpty { seed.coverUrl },
			largeCoverUrl = poster?.large ?: seed.largeCoverUrl,
			authors = authorNames,
			description = synopsisHtml?.let { Jsoup.parse(it).text() }.orEmpty(),
			tags = tags,
			state = when (status?.lowercase()) {
				"releasing" -> MangaState.ONGOING
				"finished" -> MangaState.FINISHED
				"on_hiatus" -> MangaState.PAUSED
				"discontinued" -> MangaState.ABANDONED
				else -> null
			},
			source = source.name.toMangaSourceRef(),
		)
	}

	private fun ChapterDto.toChapter(mangaUrl: String): MangaChapter {
		val numStr = number.toString().removeSuffix(".0")
		val url = "$mangaUrl/$id-chapter-$numStr-$langCode"
		return MangaChapter(
			id = "${source.name}|chapter|$url",
			title = buildString {
				append("Ch. ")
				append(numStr)
				if (!name.isNullOrBlank()) {
					append(" - ")
					append(name)
				}
			},
			number = number,
			url = url,
			scanlator = type,
			uploadDate = (createdAt ?: 0L) * 1000L,
		)
	}

	private companion object {
		const val PAGE_SIZE = 50
	}
}

// -- API DTOs ---------------------------------------------------------------
@Serializable
private class ApiResponse<T>(val items: List<T> = emptyList(), val meta: ApiMeta? = null)

@Serializable
private class ApiMeta(val lastPage: Int = 1, val hasNext: Boolean = false)

@Serializable
private class MangaDto(
	val hid: String,
	val slug: String? = null,
	val title: String,
	val poster: PosterDto? = null,
)

@Serializable
private class MangaDetailsResponse(val data: MangaDetailsDto)

@Serializable
private class MangaDetailsDto(
	val hid: String,
	val slug: String? = null,
	val title: String,
	val type: String? = null,
	val status: String? = null,
	val poster: PosterDto? = null,
	val synopsisHtml: String? = null,
	val authors: List<EntityDto>? = null,
	val artists: List<EntityDto>? = null,
	val genres: List<EntityDto>? = null,
	val themes: List<EntityDto>? = null,
)

@Serializable
private class PosterDto(val small: String? = null, val medium: String? = null, val large: String? = null)

@Serializable
private class EntityDto(val title: String)

@Serializable
private class ChapterDto(
	val id: Int,
	val number: Float,
	val name: String? = null,
	val createdAt: Long? = null,
	val type: String? = null,
)

@Serializable
private class PagesResponse(val data: ChapterDataDto)

@Serializable
private class ChapterDataDto(val pages: List<PageDto>)

@Serializable
private class PageDto(val url: String)
