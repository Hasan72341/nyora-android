package com.nyora.hasan72341.settings

import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import dagger.hilt.android.AndroidEntryPoint
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.model.ZoomMode
import com.nyora.hasan72341.core.nav.router
import com.nyora.hasan72341.core.prefs.AppSettings
import com.nyora.hasan72341.core.prefs.ReaderAnimation
import com.nyora.hasan72341.core.prefs.ReaderBackground
import com.nyora.hasan72341.core.prefs.ReaderControl
import com.nyora.hasan72341.core.prefs.ReaderMode
import com.nyora.hasan72341.core.ui.BasePreferenceFragment
import com.nyora.hasan72341.core.util.ext.setDefaultValueCompat
import com.nyora.hasan72341.mihon.parsers.util.mapToSet
import com.nyora.hasan72341.mihon.parsers.util.names
import com.nyora.hasan72341.settings.utils.MultiSummaryProvider
import com.nyora.hasan72341.settings.utils.PercentSummaryProvider
import com.nyora.hasan72341.settings.utils.SliderPreference

@AndroidEntryPoint
class ReaderSettingsFragment :
	BasePreferenceFragment(R.string.reader_settings),
	SharedPreferences.OnSharedPreferenceChangeListener {

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_reader)
		findPreference<ListPreference>(AppSettings.KEY_READER_MODE)?.run {
			entryValues = ReaderMode.entries.names()
			setDefaultValueCompat(ReaderMode.STANDARD.name)
		}
		findPreference<ListPreference>(AppSettings.KEY_READER_ORIENTATION)?.run {
			entryValues = arrayOf(
				ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED.toString(),
				ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR.toString(),
				ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT.toString(),
				ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE.toString(),
			)
			setDefaultValueCompat(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED.toString())
		}
		findPreference<MultiSelectListPreference>(AppSettings.KEY_READER_CONTROLS)?.run {
			entryValues = ReaderControl.entries.names()
			setDefaultValueCompat(ReaderControl.DEFAULT.mapToSet { it.name })
			summaryProvider = MultiSummaryProvider(R.string.none)
		}
		findPreference<ListPreference>(AppSettings.KEY_READER_BACKGROUND)?.run {
			entryValues = ReaderBackground.entries.names()
			setDefaultValueCompat(ReaderBackground.DEFAULT.name)
		}
		findPreference<ListPreference>(AppSettings.KEY_READER_ANIMATION)?.run {
			entryValues = ReaderAnimation.entries.names()
			setDefaultValueCompat(ReaderAnimation.DEFAULT.name)
		}
		findPreference<ListPreference>(AppSettings.KEY_ZOOM_MODE)?.run {
			entryValues = ZoomMode.entries.names()
			setDefaultValueCompat(ZoomMode.FIT_CENTER.name)
		}
		findPreference<MultiSelectListPreference>(AppSettings.KEY_READER_CROP)?.run {
			summaryProvider = MultiSummaryProvider(R.string.disabled)
		}
		findPreference<SliderPreference>(AppSettings.KEY_WEBTOON_ZOOM_OUT)?.summaryProvider = PercentSummaryProvider()
		updateReaderModeDependency()
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		settings.subscribe(this)
	}

	override fun onDestroyView() {
		settings.unsubscribe(this)
		super.onDestroyView()
	}

	override fun onPreferenceTreeClick(preference: Preference): Boolean {
		return when (preference.key) {
			AppSettings.KEY_READER_TAP_ACTIONS -> {
				router.openReaderTapGridSettings()
				true
			}

			else -> super.onPreferenceTreeClick(preference)
		}
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
		when (key) {
			AppSettings.KEY_READER_MODE -> updateReaderModeDependency()
		}
	}

	private fun updateReaderModeDependency() {
		findPreference<Preference>(AppSettings.KEY_READER_MODE_DETECT)?.run {
			isEnabled = settings.defaultReaderMode != ReaderMode.WEBTOON
		}
	}
}
