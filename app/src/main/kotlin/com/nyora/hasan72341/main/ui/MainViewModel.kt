package com.nyora.hasan72341.main.ui

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import com.nyora.hasan72341.core.exceptions.EmptyHistoryException
import com.nyora.hasan72341.core.github.AppUpdateRepository
import com.nyora.hasan72341.core.prefs.AppSettings
import com.nyora.hasan72341.core.prefs.observeAsFlow
import com.nyora.hasan72341.core.prefs.observeAsStateFlow
import com.nyora.hasan72341.core.ui.BaseViewModel
import com.nyora.hasan72341.core.util.ext.MutableEventFlow
import com.nyora.hasan72341.core.util.ext.call
import com.nyora.hasan72341.explore.data.MangaSourcesRepository
import com.nyora.hasan72341.history.data.HistoryRepository
import com.nyora.hasan72341.main.domain.ReadingResumeEnabledUseCase
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.tracker.domain.TrackingRepository
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
	private val historyRepository: HistoryRepository,
	private val appUpdateRepository: AppUpdateRepository,
	trackingRepository: TrackingRepository,
	private val settings: AppSettings,
	readingResumeEnabledUseCase: ReadingResumeEnabledUseCase,
	private val sourcesRepository: MangaSourcesRepository,
) : BaseViewModel() {

	val onOpenReader = MutableEventFlow<Manga>()
	val onFirstStart = MutableEventFlow<Unit>()

	val isResumeEnabled = readingResumeEnabledUseCase()
		.withErrorHandling()
		.stateIn(
			scope = viewModelScope + Dispatchers.IO,
			started = SharingStarted.WhileSubscribed(5000),
			initialValue = false,
		)

	val appUpdate = appUpdateRepository.observeAvailableUpdate()

	val feedCounter = trackingRepository.observeUnreadUpdatesCount()
		.withErrorHandling()
		.stateIn(viewModelScope + Dispatchers.IO, SharingStarted.Lazily, 0)

	val isBottomNavPinned = settings.observeAsFlow(
		AppSettings.KEY_NAV_PINNED,
	) {
		isNavBarPinned
	}.flowOn(Dispatchers.IO)

	val isIncognitoModeEnabled = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.IO,
		key = AppSettings.KEY_INCOGNITO_MODE,
		valueProducer = { isIncognitoModeEnabled },
	)

	init {
		launchJob(Dispatchers.IO) {
			if (sourcesRepository.isSetupRequired()) {
				onFirstStart.call(Unit)
			}
		}
	}

	fun openLastReader() {
		launchLoadingJob(Dispatchers.IO) {
			val manga = historyRepository.getLastOrNull() ?: throw EmptyHistoryException()
			onOpenReader.call(manga)
		}
	}

	fun setIncognitoMode(isEnabled: Boolean) {
		settings.isIncognitoModeEnabled = isEnabled
	}
}
