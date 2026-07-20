package com.nyora.hasan72341.search.domain

import android.app.SearchManager
import android.content.Context
import android.provider.SearchRecentSuggestions
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import com.nyora.hasan72341.core.db.MangaDatabase
import com.nyora.hasan72341.core.db.entity.toEntity
import com.nyora.hasan72341.core.db.entity.toManga
import com.nyora.hasan72341.core.prefs.AppSettings
import com.nyora.hasan72341.core.model.getContentTypeOrNull
import com.nyora.hasan72341.core.model.titleOrName
import com.nyora.hasan72341.explore.data.MangaSourcesRepository
import com.nyora.hasan72341.mihon.parsers.model.ContentType
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.mihon.parsers.model.MangaSource
import com.nyora.hasan72341.mihon.parsers.model.MangaTag
import com.nyora.hasan72341.mihon.parsers.util.levenshteinDistance
import com.nyora.hasan72341.mihon.parsers.util.mapToSet
import com.nyora.hasan72341.search.ui.MangaSuggestionsProvider
import javax.inject.Inject

@Reusable
class MangaSearchRepository @Inject constructor(
	private val db: MangaDatabase,
	private val sourcesRepository: MangaSourcesRepository,
	@ApplicationContext private val context: Context,
	private val recentSuggestions: SearchRecentSuggestions,
	private val settings: AppSettings,
) {

	suspend fun getMangaSuggestion(query: String, limit: Int, source: MangaSource?): List<Manga> = when {
		query.isEmpty() -> db.getSuggestionDao().getTopManga(limit)
		source != null -> db.getMangaDao().searchByTitle("%$query%", source.name, limit)
		else -> db.getMangaDao().searchByTitle("%$query%", limit)
	}.let {
		if (settings.isNsfwContentDisabled) it.filterNot { x -> x.isNsfw } else it
	}.map {
		it.toManga()
	}.sortedBy { x ->
		x.title.levenshteinDistance(query)
	}

	suspend fun getQuerySuggestion(
		query: String,
		limit: Int,
	): List<String> = withContext(Dispatchers.IO) {
		context.contentResolver.query(
			MangaSuggestionsProvider.QUERY_URI,
			arrayOf(SearchManager.SUGGEST_COLUMN_QUERY),
			"${SearchManager.SUGGEST_COLUMN_QUERY} LIKE ?",
			arrayOf("%$query%"),
			"date DESC",
		)?.use { cursor ->
			val count = minOf(cursor.count, limit)
			if (count == 0) {
				return@withContext emptyList()
			}
			val result = ArrayList<String>(count)
			if (cursor.moveToFirst()) {
				val index = cursor.getColumnIndexOrThrow(SearchManager.SUGGEST_COLUMN_QUERY)
				do {
					result += cursor.getString(index)
				} while (currentCoroutineContext().isActive && cursor.moveToNext())
			}
			result
		}.orEmpty()
	}

	suspend fun getQueryHintSuggestion(
		query: String,
		limit: Int,
	): List<String> {
		if (query.isEmpty()) {
			return emptyList()
		}
		val titles = db.getSuggestionDao().getTitles("$query%")
		if (titles.isEmpty()) {
			return emptyList()
		}
		return titles.shuffled().take(limit)
	}

	suspend fun getAuthorsSuggestion(
		query: String,
		limit: Int,
	): List<String> {
		if (query.isEmpty()) {
			return emptyList()
		}
		return db.getMangaDao().findAuthors("$query%", limit)
	}

	suspend fun getTagsSuggestion(query: String, limit: Int, source: MangaSource?): List<MangaTag> {
		return emptyList()
	}

	suspend fun getTagsSuggestion(tags: Set<MangaTag>): List<MangaTag> {
		return emptyList()
	}

	suspend fun getRareTags(source: MangaSource, limit: Int): List<MangaTag> {
		return emptyList()
	}

	suspend fun getTopTags(source: MangaSource, limit: Int): List<MangaTag> {
		return emptyList()
	}

	suspend fun getSourcesSuggestion(limit: Int): List<MangaSource> = sourcesRepository.getTopSources(limit)

	fun getSourcesSuggestion(query: String, limit: Int): List<MangaSource> {
		if (query.length < 3) {
			return emptyList()
		}
		if (!settings.isSourcesUnlocked) {
			return emptyList()
		}
		val skipNsfw = settings.isNsfwContentDisabled
		val sources = sourcesRepository.allMangaSources
			.filter { x ->
				(x.getContentTypeOrNull() !in HENTAI_CONTENT_TYPES || !skipNsfw) && x.titleOrName().contains(query, ignoreCase = true)
			}
		return if (limit == 0) {
			sources
		} else {
			sources.take(limit)
		}
	}

	fun saveSearchQuery(query: String) {
		recentSuggestions.saveRecentQuery(query, null)
	}

	suspend fun clearSearchHistory(): Unit = withContext(Dispatchers.IO) {
		recentSuggestions.clearHistory()
	}

	suspend fun deleteSearchQuery(query: String) = withContext(Dispatchers.IO) {
		context.contentResolver.delete(
			MangaSuggestionsProvider.URI,
			"display1 = ?",
			arrayOf(query),
		)
	}

	suspend fun getSearchHistoryCount(): Int = withContext(Dispatchers.IO) {
		context.contentResolver.query(
			MangaSuggestionsProvider.QUERY_URI,
			arrayOf(SearchManager.SUGGEST_COLUMN_QUERY),
			null,
			arrayOfNulls(1),
			null,
		)?.use { cursor -> cursor.count } ?: 0
	}

    suspend fun getAuthors(source: MangaSource, limit: Int): List<String> {
        return db.getMangaDao().findAuthorsBySource(source.name, limit)
    }

	private companion object {
		val HENTAI_CONTENT_TYPES = setOf(
			ContentType.HENTAI_MANGA,
			ContentType.HENTAI_NOVEL,
			ContentType.HENTAI_VIDEO,
		)
	}
}
