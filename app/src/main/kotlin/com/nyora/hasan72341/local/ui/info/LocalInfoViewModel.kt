package com.nyora.hasan72341.local.ui.info

import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import com.nyora.hasan72341.core.model.parcelable.ParcelableManga
import com.nyora.hasan72341.core.nav.AppRouter
import com.nyora.hasan72341.core.ui.BaseViewModel
import com.nyora.hasan72341.core.util.ext.MutableEventFlow
import com.nyora.hasan72341.core.util.ext.call
import com.nyora.hasan72341.core.util.ext.computeSize
import com.nyora.hasan72341.core.util.ext.require
import com.nyora.hasan72341.core.util.ext.toFileOrNull
import com.nyora.hasan72341.local.data.LocalMangaRepository
import com.nyora.hasan72341.local.data.LocalStorageManager
import com.nyora.hasan72341.local.domain.DeleteReadChaptersUseCase
import javax.inject.Inject

@HiltViewModel
class LocalInfoViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val localMangaRepository: LocalMangaRepository,
	private val storageManager: LocalStorageManager,
	private val deleteReadChaptersUseCase: DeleteReadChaptersUseCase,
) : BaseViewModel() {

	private val manga = savedStateHandle.require<ParcelableManga>(AppRouter.KEY_MANGA).manga

	val isCleaningUp = MutableStateFlow(false)
	val onCleanedUp = MutableEventFlow<Pair<Int, Long>>()

	val path = MutableStateFlow<String?>(null)
	val size = MutableStateFlow(-1L)
	val availableSize = MutableStateFlow(-1L)

	init {
		computeSize()
	}

	fun cleanup() {
		launchJob(Dispatchers.IO) {
			try {
				isCleaningUp.value = true
				val oldSize = size.value
				val chaptersCount = deleteReadChaptersUseCase.invoke(manga)
				computeSize().join()
				val newSize = size.value
				onCleanedUp.call(chaptersCount to oldSize - newSize)
			} finally {
				isCleaningUp.value = false
			}
		}
	}

	private fun computeSize() = launchLoadingJob(Dispatchers.IO) {
		val file = manga.url.toUri().toFileOrNull() ?: localMangaRepository.findSavedManga(manga)?.file
		requireNotNull(file)
		path.value = file.path
		size.value = file.computeSize()
		availableSize.value = storageManager.computeAvailableSize()
	}
}
