package com.nyora.hasan72341.suggestions.domain

import com.nyora.hasan72341.explore.data.MangaSourcesRepository
import com.nyora.hasan72341.search.domain.SearchKind
import com.nyora.hasan72341.search.domain.SearchV2Helper
import com.nyora.hasan72341.suggestions.data.AnilistMedia
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.mihon.parsers.model.MangaSource
import com.nyora.hasan72341.mihon.parsers.util.levenshteinDistance
import javax.inject.Inject

class SmartMatchUseCase @Inject constructor(
    private val searchHelperFactory: SearchV2Helper.Factory,
    private val sourcesRepository: MangaSourcesRepository,
) {
    private val semaphore = Semaphore(5)

    suspend fun findMatches(media: AnilistMedia): List<Manga> = coroutineScope {
        val title = media.title.preferred()
        val sources = sourcesRepository.getEnabledSources()
        
        sources.map { source ->
            async {
                semaphore.withPermit {
                    searchSource(source, title)
                }
            }
        }.awaitAll()
            .flatten()
            .distinctBy { it.url }
            .sortedBy { it.title.levenshteinDistance(title) }
            .take(5)
    }

    private suspend fun searchSource(source: MangaSource, query: String): List<Manga> {
        return try {
            val searchHelper = searchHelperFactory.create(source)
            val result = searchHelper(query, SearchKind.SIMPLE)
            result?.manga ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
