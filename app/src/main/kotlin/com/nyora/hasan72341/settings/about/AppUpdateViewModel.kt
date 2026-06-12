package com.nyora.hasan72341.settings.about

import dagger.hilt.android.lifecycle.HiltViewModel
import com.nyora.hasan72341.core.github.AppUpdateRepository
import com.nyora.hasan72341.core.ui.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class AppUpdateViewModel @Inject constructor(
	private val repository: AppUpdateRepository,
) : BaseViewModel() {

	val nextVersion = repository.observeAvailableUpdate()

	init {
		if (nextVersion.value == null) {
			launchLoadingJob {
				repository.fetchUpdate()
			}
		}
	}
}
