package com.nyora.hasan72341.core.parser

import kotlinx.coroutines.Dispatchers
import okhttp3.Interceptor
import okhttp3.Response
import com.nyora.hasan72341.core.cache.MemoryContentCache
import com.nyora.hasan72341.core.exceptions.CloudFlareProtectedException
import com.nyora.hasan72341.core.exceptions.InteractiveActionRequiredException
import com.nyora.hasan72341.core.exceptions.ProxyConfigException
import com.nyora.hasan72341.core.prefs.SourceSettings
import com.nyora.hasan72341.mihon.parsers.MangaParser
import com.nyora.hasan72341.mihon.parsers.MangaParserAuthProvider
import com.nyora.hasan72341.mihon.parsers.config.ConfigKey
import com.nyora.hasan72341.mihon.parsers.exception.AuthRequiredException
import com.nyora.hasan72341.mihon.parsers.model.Favicon
import com.nyora.hasan72341.mihon.parsers.model.Favicons
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.mihon.parsers.model.MangaChapter
import com.nyora.hasan72341.mihon.parsers.model.MangaListFilter
import com.nyora.hasan72341.mihon.parsers.model.MangaListFilterCapabilities
import com.nyora.hasan72341.mihon.parsers.model.MangaListFilterOptions
import com.nyora.hasan72341.mihon.parsers.model.MangaPage
import com.nyora.hasan72341.mihon.parsers.model.MangaParserSource
import com.nyora.hasan72341.mihon.parsers.model.SortOrder
import com.nyora.hasan72341.mihon.parsers.util.runCatchingCancellable
import com.nyora.hasan72341.mihon.parsers.util.suspendlazy.suspendLazy
import com.nyora.hasan72341.core.model.toMangaSourceRef
import com.nyora.hasan72341.mihon.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.config.ConfigKey as LibConfigKey
import org.koitharu.kotatsu.parsers.model.ContentRating as LibContentRating
import org.koitharu.kotatsu.parsers.model.Manga as LibManga
import org.koitharu.kotatsu.parsers.model.MangaChapter as LibMangaChapter
import org.koitharu.kotatsu.parsers.model.MangaPage as LibMangaPage
import org.koitharu.kotatsu.parsers.model.MangaState as LibMangaState
import org.koitharu.kotatsu.parsers.model.MangaTag as LibMangaTag
import org.koitharu.kotatsu.parsers.model.SortOrder as LibSortOrder
import com.nyora.hasan72341.mihon.parsers.model.ContentRating
import com.nyora.hasan72341.mihon.parsers.model.MangaState

class ParserMangaRepository(
	private val parser: MangaParser,
	private val mirrorSwitcher: MirrorSwitcher,
	cache: MemoryContentCache,
) : CachingMangaRepository(cache), Interceptor {

	private val filterOptionsLazy = suspendLazy(Dispatchers.IO) {
		withMirrors {
			parser.getFilterOptions()
		}
	}

	override val source: MangaParserSource
		get() = parser.source

	override val sortOrders: Set<SortOrder>
		get() = parser.availableSortOrders.mapTo(LinkedHashSet()) { it.toNyora() }

	override val filterCapabilities: MangaListFilterCapabilities
		get() = parser.filterCapabilities

	override var defaultSortOrder: SortOrder
		get() = getConfig().defaultSortOrder ?: sortOrders.first()
		set(value) {
			getConfig().defaultSortOrder = value
		}

	var domain: String
		get() = parser.domain
		set(value) {
			getConfig()[ConfigKey.Domain(*parser.configKeyDomain.presetValues)] = value
		}

	val domains: Array<out String>
		get() = parser.configKeyDomain.presetValues

	override fun intercept(chain: Interceptor.Chain): Response = parser.intercept(chain)

	override suspend fun getList(offset: Int, order: SortOrder?, filter: MangaListFilter?): List<Manga> {
		return withMirrors {
			parser.getList(offset, (order ?: defaultSortOrder).toLib(), filter ?: MangaListFilter.EMPTY)
				.map { it.toNyora() }
		}
	}

	override suspend fun getPagesImpl(
		chapter: MangaChapter
	): List<MangaPage> = withMirrors {
		parser.getPages(chapter.toLib()).map { it.toNyora().copy(source = parser.source) }
	}

	override suspend fun getPageUrl(page: MangaPage): String = withMirrors {
		parser.getPageUrl(page.toLib()).also { result ->
			check(result.isNotEmpty()) { "Page url is empty" }
		}
	}

	override suspend fun getFilterOptions(): MangaListFilterOptions = filterOptionsLazy.get()

	suspend fun getFavicons(): Favicons = withMirrors {
		val libFavicons = parser.getFavicons()
		Favicons(
			favicons = libFavicons.map { Favicon(it.url, it.size, null) },
			referer = libFavicons.referer,
		)
	}

	override suspend fun getRelatedMangaImpl(seed: Manga): List<Manga> =
		parser.getRelatedManga(seed.toLib()).map { it.toNyora() }

	override suspend fun getDetailsImpl(manga: Manga): Manga = withMirrors {
		parser.getDetails(manga.toLib()).toNyora()
	}

	fun getAuthProvider(): MangaParserAuthProvider? = parser.authorizationProvider

	fun getRequestHeaders() = parser.getRequestHeaders()

	fun getConfigKeys(): List<ConfigKey<*>> = ArrayList<LibConfigKey<*>>().also {
		parser.onCreateConfig(it)
	}.mapNotNull { it.toNyoraConfigKey() }

	fun getAvailableMirrors(): List<String> {
		return parser.configKeyDomain.presetValues.toList()
	}

	fun isSlowdownEnabled(): Boolean {
		return getConfig().isSlowdownEnabled
	}

	fun getConfig() = parser.config as SourceSettings

	private suspend fun <T : Any> withMirrors(block: suspend () -> T): T {
		if (!mirrorSwitcher.isEnabled) {
			return block()
		}
		val initialResult = runCatchingCancellable { block() }
		if (initialResult.isValidResult()) {
			return initialResult.getOrThrow()
		}
		val newResult = mirrorSwitcher.trySwitchMirror(this, block)
		return newResult ?: initialResult.getOrThrow()
	}

	private fun Result<Any>.isValidResult() = fold(
		onSuccess = {
			when (it) {
				is Collection<*> -> it.isNotEmpty()
				else -> true
			}
		},
		onFailure = {
			when (it.cause) {
				is CloudFlareProtectedException,
				is AuthRequiredException,
				is InteractiveActionRequiredException,
				is ProxyConfigException -> true

				else -> false
			}
		},
	)

	// region lib <-> native model conversions (lib-parser call boundary)

	private fun LibSortOrder.toNyora(): SortOrder = SortOrder.valueOf(name)

	private fun SortOrder.toLib(): LibSortOrder =
		LibSortOrder.entries.firstOrNull { it.name == name } ?: parser.availableSortOrders.first()

	private fun LibContentRating.toNyora(): ContentRating = ContentRating.valueOf(name)

	private fun ContentRating.toLib(): LibContentRating = LibContentRating.valueOf(name)

	private fun LibMangaState.toNyora(): MangaState = MangaState.valueOf(name)

	private fun MangaState.toLib(): LibMangaState = LibMangaState.valueOf(name)

	private fun LibMangaTag.toNyora(): MangaTag = MangaTag(key = key, title = title)

	private fun MangaTag.toLib(): LibMangaTag = LibMangaTag(title, key, parser.source)

	private fun LibManga.toNyora(): Manga = Manga(
		id = id.toString(),
		title = title,
		altTitles = altTitles.toList(),
		url = url,
		publicUrl = publicUrl,
		rating = rating,
		isNsfw = isNsfw,
		contentRating = contentRating?.toNyora(),
		coverUrl = coverUrl.orEmpty(),
		largeCoverUrl = largeCoverUrl,
		state = state?.toNyora(),
		authors = authors.toList(),
		source = source.name.toMangaSourceRef(),
		description = description.orEmpty(),
		tags = tags.map { it.toNyora() },
		chapters = chapters.orEmpty().map { it.toNyora() },
	)

	private fun Manga.toLib(): LibManga = LibManga(
		/* id = */ id.toLongOrNull() ?: 0L,
		/* title = */ title,
		/* altTitles = */ altTitles.toSet(),
		/* url = */ url,
		/* publicUrl = */ publicUrl,
		/* rating = */ rating,
		/* contentRating = */ contentRating?.toLib(),
		/* coverUrl = */ coverUrl,
		/* tags = */ tags.mapTo(LinkedHashSet()) { it.toLib() },
		/* state = */ state?.toLib(),
		/* authors = */ authors.toSet(),
		/* largeCoverUrl = */ largeCoverUrl,
		/* description = */ description,
		/* chapters = */ chapters.map { it.toLib() },
		/* source = */ parser.source,
	)

	private fun LibMangaChapter.toNyora(): MangaChapter = MangaChapter(
		id = id.toString(),
		title = title.orEmpty(),
		number = number,
		volume = volume,
		url = url,
		scanlator = scanlator,
		uploadDate = uploadDate,
		branch = branch,
	)

	private fun MangaChapter.toLib(): LibMangaChapter = LibMangaChapter(
		/* id = */ id.toLongOrNull() ?: 0L,
		/* name = */ title,
		/* number = */ number,
		/* volume = */ volume,
		/* url = */ url,
		/* scanlator = */ scanlator,
		/* uploadDate = */ uploadDate,
		/* branch = */ branch,
		/* source = */ parser.source,
	)

	private fun LibMangaPage.toNyora(): MangaPage = MangaPage(
		url = url,
		id = id.toString(),
		preview = preview,
	)

	private fun MangaPage.toLib(): LibMangaPage = LibMangaPage(
		/* id = */ id.toLongOrNull() ?: 0L,
		/* url = */ url,
		/* preview = */ preview,
		/* source = */ parser.source,
	)

	private fun LibConfigKey<*>.toNyoraConfigKey(): ConfigKey<*>? = when (this) {
		is LibConfigKey.Domain -> ConfigKey.Domain(*presetValues)
		is LibConfigKey.UserAgent -> ConfigKey.UserAgent(defaultValue)
		is LibConfigKey.ShowSuspiciousContent -> ConfigKey.ShowSuspiciousContent(defaultValue)
		is LibConfigKey.SplitByTranslations -> ConfigKey.SplitByTranslations(defaultValue)
		is LibConfigKey.PreferredImageServer -> ConfigKey.PreferredImageServer(
			presetValues.entries.associate { (k, v) -> k as String? to v as String? },
			defaultValue,
		)
		else -> null
	}

	// endregion
}
