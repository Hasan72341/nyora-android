package com.nyora.hasan72341.settings.about.changelog

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import com.nyora.hasan72341.core.github.AppUpdateRepository
import com.nyora.hasan72341.core.ui.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class ChangelogViewModel @Inject constructor(
	private val appUpdateRepository: AppUpdateRepository,
) : BaseViewModel() {

	val changelog = MutableStateFlow<String?>(null)

	init {
		launchLoadingJob(Dispatchers.IO) {
			changelog.value = appUpdateRepository.fetchChangelog()
		}
	}
}
