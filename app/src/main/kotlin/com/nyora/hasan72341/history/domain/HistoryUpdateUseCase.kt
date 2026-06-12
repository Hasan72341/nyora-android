package com.nyora.hasan72341.history.domain

import com.nyora.hasan72341.core.util.ext.printStackTraceDebug
import com.nyora.hasan72341.core.util.ext.processLifecycleScope
import com.nyora.hasan72341.history.data.HistoryRepository
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.mihon.parsers.util.runCatchingCancellable
import com.nyora.hasan72341.reader.ui.ReaderState
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class HistoryUpdateUseCase @Inject constructor(
	private val historyRepository: HistoryRepository,
) {

	suspend operator fun invoke(manga: Manga, readerState: ReaderState, percent: Float) {
		historyRepository.addOrUpdate(
			manga = manga,
			chapterId = readerState.chapterId,
			page = readerState.page,
			scroll = readerState.scroll,
			percent = percent,
			force = false,
		)
	}

	fun invokeAsync(
		manga: Manga,
		readerState: ReaderState,
		percent: Float
	) = processLifecycleScope.launch(Dispatchers.IO, CoroutineStart.ATOMIC) {
		runCatchingCancellable {
			withContext(NonCancellable) {
				invoke(manga, readerState, percent)
			}
		}.onFailure {
			it.printStackTraceDebug("HistoryUpdateUseCase::invokeAsync")
		}
	}
}
