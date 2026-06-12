package com.nyora.hasan72341.details.domain

import com.nyora.hasan72341.core.parser.MangaRepository
import com.nyora.hasan72341.core.model.toMangaSource
import com.nyora.hasan72341.core.util.ext.printStackTraceDebug
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.mihon.parsers.util.runCatchingCancellable
import javax.inject.Inject

class RelatedMangaUseCase @Inject constructor(
	private val mangaRepositoryFactory: MangaRepository.Factory,
) {

	suspend operator fun invoke(seed: Manga) = runCatchingCancellable {
		mangaRepositoryFactory.create(seed.source.toMangaSource()).getRelated(seed)
	}.onFailure {
		it.printStackTraceDebug("RelatedMangaUseCase::invoke")
	}.getOrNull()
}
