package com.nyora.hasan72341.settings.work

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.nyora.hasan72341.core.prefs.AppSettings
import com.nyora.hasan72341.core.util.ext.processLifecycleScope
import com.nyora.hasan72341.suggestions.ui.SuggestionsWorker
import com.nyora.hasan72341.tracker.work.TrackWorker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkScheduleManager @Inject constructor(
	private val settings: AppSettings,
	private val suggestionScheduler: SuggestionsWorker.Scheduler,
	private val trackerScheduler: TrackWorker.Scheduler,
) : SharedPreferences.OnSharedPreferenceChangeListener {

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
		when (key) {
			AppSettings.KEY_TRACKER_ENABLED,
			AppSettings.KEY_TRACKER_FREQUENCY,
			AppSettings.KEY_TRACKER_WIFI_ONLY -> updateWorker(
				scheduler = trackerScheduler,
				isEnabled = settings.isTrackerEnabled,
				force = key != AppSettings.KEY_TRACKER_ENABLED,
			)

			AppSettings.KEY_SUGGESTIONS,
			AppSettings.KEY_SUGGESTIONS_WIFI_ONLY -> updateWorker(
				scheduler = suggestionScheduler,
				isEnabled = settings.isSuggestionsEnabled,
				force = key != AppSettings.KEY_SUGGESTIONS,
			)
		}
	}

	fun init() {
		settings.subscribe(this)
		processLifecycleScope.launch(Dispatchers.IO) {
			updateWorkerImpl(trackerScheduler, settings.isTrackerEnabled, true) // always force due to adaptive interval
			updateWorkerImpl(suggestionScheduler, settings.isSuggestionsEnabled, false)
		}
	}

	private fun updateWorker(scheduler: PeriodicWorkScheduler, isEnabled: Boolean, force: Boolean) {
		processLifecycleScope.launch(Dispatchers.IO) {
			updateWorkerImpl(scheduler, isEnabled, force)
		}
	}

	private suspend fun updateWorkerImpl(scheduler: PeriodicWorkScheduler, isEnabled: Boolean, force: Boolean) {
		if (force || scheduler.isScheduled() != isEnabled) {
			if (isEnabled) {
				scheduler.schedule()
			} else {
				scheduler.unschedule()
			}
		}
	}
}
