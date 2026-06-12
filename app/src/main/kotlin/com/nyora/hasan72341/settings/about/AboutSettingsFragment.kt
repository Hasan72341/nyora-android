package com.nyora.hasan72341.settings.about

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.nyora.hasan72341.BuildConfig
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.ui.BasePreferenceFragment
import com.nyora.hasan72341.js.NyoraJsOtaUpdater
import javax.inject.Inject

@AndroidEntryPoint
class AboutSettingsFragment : BasePreferenceFragment(R.string.about) {

	@Inject
	lateinit var otaUpdater: NyoraJsOtaUpdater

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_about)
		findPreference<Preference>("about_app_info")?.summary = "v${BuildConfig.VERSION_NAME}"
		updateParserSummary()
	}

	override fun onPreferenceTreeClick(preference: Preference): Boolean = when (preference.key) {
		"about_parser_info" -> {
			checkForParserUpdates()
			true
		}
		else -> super.onPreferenceTreeClick(preference)
	}

	private fun updateParserSummary() {
		val version = otaUpdater.localVersion()
		val isOta = otaUpdater.isActive()
		val summaryText = if (isOta) {
			"v$version (OTA)"
		} else {
			"v$version (" + getString(R.string.bundled) + ")"
		}
		findPreference<Preference>("about_parser_info")?.summary = summaryText
	}

	private fun checkForParserUpdates() {
		viewLifecycleOwner.lifecycleScope.launch {
			val pref = findPreference<Preference>("about_parser_info")
			pref?.isEnabled = false
			pref?.summary = getString(R.string.checking_for_updates)
			
			val result = withContext(Dispatchers.IO) {
				runCatching { otaUpdater.updateOnce() }
			}

			if (result.isSuccess) {
				updateParserSummary()
				val message = if (result.getOrDefault(false)) {
					R.string.parsers_updated_restart
				} else {
					R.string.parsers_already_current
				}
				Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show()
			} else {
				updateParserSummary()
				Snackbar.make(requireView(), R.string.error_checking_updates, Snackbar.LENGTH_SHORT).show()
			}
			pref?.isEnabled = true
		}
	}
}
