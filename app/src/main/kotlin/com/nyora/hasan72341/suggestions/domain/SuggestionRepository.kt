package com.nyora.hasan72341.suggestions.domain

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import com.nyora.hasan72341.core.db.MangaDatabase
import com.nyora.hasan72341.core.db.entity.toEntity
import com.nyora.hasan72341.core.db.entity.toManga
import com.nyora.hasan72341.core.model.toMangaSources
import com.nyora.hasan72341.core.util.ext.mapItems
import com.nyora.hasan72341.list.domain.ListFilterOption
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.mihon.parsers.model.MangaSource
import com.nyora.hasan72341.mihon.parsers.model.MangaTag
import com.nyora.hasan72341.suggestions.data.SuggestionEntity
import com.nyora.hasan72341.suggestions.data.SuggestionWithManga
import javax.inject.Inject

class SuggestionRepository @Inject constructor(
	private val db: MangaDatabase,
) {

	fun observeAll(): Flow<List<Manga>> {
		return db.getSuggestionDao().observeAll().mapItems {
			it.toManga()
		}
	}

	fun observeAll(limit: Int, filterOptions: Set<ListFilterOption>): Flow<List<Manga>> {
		return db.getSuggestionDao().observeAll(limit, filterOptions).mapItems {
			it.toManga()
		}
	}

	suspend fun getRandomList(limit: Int): List<Manga> {
		return db.getSuggestionDao().getRandom(limit).map {
			it.toManga()
		}
	}

	suspend fun clear() {
		db.getSuggestionDao().deleteAll()
	}

	suspend fun isEmpty(): Boolean {
		return db.getSuggestionDao().count() == 0
	}

	suspend fun getTopTags(limit: Int): List<MangaTag> {
		return emptyList()
	}

	suspend fun getTopSources(limit: Int): List<MangaSource> {
		return db.getSuggestionDao().getTopSources(limit)
			.toMangaSources()
	}

	suspend fun replace(suggestions: Iterable<MangaSuggestion>) {
		db.withTransaction {
			db.getSuggestionDao().deleteAll()
			suggestions.forEach { (manga, relevance) ->
				db.getMangaDao().upsert(manga.toEntity())
				db.getSuggestionDao().upsert(
					SuggestionEntity(
						mangaId = manga.id,
						relevance = relevance,
						createdAt = System.currentTimeMillis(),
					),
				)
			}
		}
	}

	private fun SuggestionWithManga.toManga(): Manga = manga.toManga()
}
