package com.nyora.hasan72341.alternatives.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import com.nyora.hasan72341.core.model.toMangaSource
import com.nyora.hasan72341.core.parser.MangaRepository
import com.nyora.hasan72341.core.util.ext.toLocale
import com.nyora.hasan72341.explore.data.MangaSourcesRepository
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.mihon.parsers.model.MangaParserSource
import com.nyora.hasan72341.mihon.parsers.model.MangaSource
import com.nyora.hasan72341.mihon.parsers.model.MangaSourceRef
import com.nyora.hasan72341.mihon.parsers.util.runCatchingCancellable
import com.nyora.hasan72341.search.domain.SearchKind
import com.nyora.hasan72341.search.domain.SearchV2Helper
import java.util.Locale
import javax.inject.Inject

private const val MAX_PARALLELISM = 4

class AlternativesUseCase @Inject constructor(
	private val sourcesRepository: MangaSourcesRepository,
	private val searchHelperFactory: SearchV2Helper.Factory,
	private val mangaRepositoryFactory: MangaRepository.Factory,
) {

	suspend operator fun invoke(manga: Manga, throughDisabledSources: Boolean): Flow<Manga> {
		val sources = getSources(manga.source, throughDisabledSources)
		if (sources.isEmpty()) {
			return emptyFlow()
		}
		val semaphore = Semaphore(MAX_PARALLELISM)
		return channelFlow {
			for (source in sources) {
				launch {
					val searchHelper = searchHelperFactory.create(source)
					val list = runCatchingCancellable {
						semaphore.withPermit {
							searchHelper(manga.title, SearchKind.TITLE)?.manga
						}
					}.getOrNull()
					list?.forEach { m ->
						if (m.id != manga.id) {
							launch {
								val details = runCatchingCancellable {
									mangaRepositoryFactory.create(m.source.toMangaSource()).getDetails(m)
								}.getOrDefault(m)
								send(details)
							}
						}
					}
				}
			}
		}
	}

	private suspend fun getSources(ref: MangaSourceRef, disabled: Boolean): List<MangaSource> = if (disabled) {
		sourcesRepository.getDisabledSources()
	} else {
		sourcesRepository.getEnabledSources()
	}.sortedByDescending { it.priority(ref) }

	private fun MangaSource.priority(ref: MangaSourceRef): Int {
		var res = 0
		val resolvedRef = ref.toMangaSource()
		if (this is MangaParserSource && resolvedRef is MangaParserSource) {
			if (locale == resolvedRef.locale) {
				res += 4
			} else if (locale.toLocale() == Locale.getDefault()) {
				res += 2
			}
			if (contentType == resolvedRef.contentType) {
				res++
			}
		}
		return res
	}
}
