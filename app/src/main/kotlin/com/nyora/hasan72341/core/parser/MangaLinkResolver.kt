package com.nyora.hasan72341.core.parser

import android.net.Uri
import coil3.request.CachePolicy
import dagger.Reusable
import com.nyora.hasan72341.core.model.MangaSource
import com.nyora.hasan72341.core.model.UnknownMangaSource
import com.nyora.hasan72341.core.model.isNsfw
import com.nyora.hasan72341.core.model.toMangaSourceRef
import com.nyora.hasan72341.core.util.ext.isHttpUrl
import com.nyora.hasan72341.mihon.parsers.MangaLoaderContext
import com.nyora.hasan72341.mihon.parsers.exception.NotFoundException
import com.nyora.hasan72341.mihon.parsers.model.ContentRating
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.mihon.parsers.model.MangaChapter
import com.nyora.hasan72341.mihon.parsers.model.MangaListFilter
import com.nyora.hasan72341.mihon.parsers.model.MangaSource
import com.nyora.hasan72341.mihon.parsers.model.MangaState
import com.nyora.hasan72341.mihon.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.model.Manga as LibManga
import com.nyora.hasan72341.mihon.parsers.util.almostEquals
import com.nyora.hasan72341.mihon.parsers.util.ifNullOrEmpty
import com.nyora.hasan72341.mihon.parsers.util.levenshteinDistance
import com.nyora.hasan72341.mihon.parsers.util.runCatchingCancellable
import javax.inject.Inject

@Reusable
class MangaLinkResolver @Inject constructor(
	private val repositoryFactory: MangaRepository.Factory,
	private val dataRepository: MangaDataRepository,
	private val context: MangaLoaderContext,
) {

	suspend fun resolve(uri: Uri): Manga {
		return if (uri.scheme == "nyora" || uri.host == "nyoraapp.pages.dev") {
			resolveAppLink(uri)
		} else {
			resolveExternalLink(uri.toString())
		} ?: throw NotFoundException("Cannot resolve link", uri.toString())
	}

	private suspend fun resolveAppLink(uri: Uri): Manga? {
		require(uri.pathSegments.singleOrNull() == "manga") { "Invalid url" }
		uri.getQueryParameter("id")?.let { mangaId ->
			// short url
			return dataRepository.findMangaById(mangaId, withChapters = false)
		}
		val sourceName = requireNotNull(uri.getQueryParameter("source")) { "Source is not specified" }
		val source = MangaSource(sourceName)
		require(source != UnknownMangaSource) { "Manga source $sourceName is not supported" }
		val repo = repositoryFactory.create(source)
		return repo.findExact(
			url = uri.getQueryParameter("url"),
			title = uri.getQueryParameter("name"),
		)
	}

	private suspend fun resolveExternalLink(uri: String): Manga? {
		dataRepository.findMangaByPublicUrl(uri)?.let {
			return it
		}
		return context.newLinkResolver(uri).getManga()?.toNativeManga()
	}

	private fun LibManga.toNativeManga(): Manga = Manga(
		id = id.toString(),
		title = title,
		altTitles = altTitles.toList(),
		url = url,
		publicUrl = publicUrl,
		rating = rating,
		isNsfw = isNsfw,
		contentRating = contentRating?.let { ContentRating.valueOf(it.name) },
		coverUrl = coverUrl.orEmpty(),
		largeCoverUrl = largeCoverUrl,
		state = state?.let { MangaState.valueOf(it.name) },
		authors = authors.toList(),
		source = source.name.toMangaSourceRef(),
		description = description.orEmpty(),
		tags = tags.map { MangaTag(key = it.key, title = it.title) },
		chapters = chapters.orEmpty().map { ch ->
			MangaChapter(
				id = ch.id.toString(),
				title = ch.title.orEmpty(),
				number = ch.number,
				volume = ch.volume,
				url = ch.url,
				scanlator = ch.scanlator,
				uploadDate = ch.uploadDate,
				branch = ch.branch,
			)
		},
	)

	private suspend fun MangaRepository.findExact(url: String?, title: String?): Manga? {
		if (!title.isNullOrEmpty()) {
			val list = getList(0, null, MangaListFilter(query = title))
			if (url != null) {
				list.find { it.url == url }?.let {
					return it
				}
			}
			list.minByOrNull { it.title.levenshteinDistance(title) }
				?.takeIf { it.title.almostEquals(title, 0.2f) }
				?.let { return it }
		}
		val seed = getDetailsNoCache(
			getSeedManga(source, url ?: return null, title),
		)
		return runCatchingCancellable {
			val seedTitle = seed.title.ifEmpty {
				seed.altTitles.firstOrNull()
			}.ifNullOrEmpty {
				seed.authors.firstOrNull()
			} ?: return@runCatchingCancellable null
			val seedList = getList(0, null, MangaListFilter(query = seedTitle))
			seedList.first { x -> x.url == url }
		}.getOrThrow()
	}

	private suspend fun MangaRepository.getDetailsNoCache(manga: Manga): Manga = if (this is CachingMangaRepository) {
		getDetails(manga, CachePolicy.READ_ONLY)
	} else {
		getDetails(manga)
	}

	private fun getSeedManga(source: MangaSource, url: String, title: String?) = Manga(
		id = run {
			var h = 1125899906842597L
			source.name.forEach { c ->
				h = 31 * h + c.code
			}
			url.forEach { c ->
				h = 31 * h + c.code
			}
			h.toString()
		},
		title = title.orEmpty(),
		url = url,
		publicUrl = "",
		rating = 0.0f,
		isNsfw = source.isNsfw(),
		coverUrl = "",
		tags = emptyList(),
		state = null,
		largeCoverUrl = null,
		description = "",
		chapters = emptyList(),
		source = source.name.toMangaSourceRef(),
	)

	companion object {

		fun isValidLink(str: String): Boolean {
			return str.isHttpUrl() || str.startsWith("nyora://", ignoreCase = true)
		}
	}
}
