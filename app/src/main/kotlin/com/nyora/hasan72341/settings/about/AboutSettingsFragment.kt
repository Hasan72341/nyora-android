package com.nyora.hasan72341.settings.about

import android.os.Bundle
import androidx.preference.Preference
import dagger.hilt.android.AndroidEntryPoint
import com.nyora.hasan72341.BuildConfig
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.ui.BasePreferenceFragment

@AndroidEntryPoint
class AboutSettingsFragment : BasePreferenceFragment(R.string.about) {

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_about)
		findPreference<Preference>("about_app_info")?.summary = "v${BuildConfig.VERSION_NAME}"
		// Parsers are now compiled in natively (kotatsu-parsers-redo); no OTA bundle.
		findPreference<Preference>("about_parser_info")?.apply {
			summary = getString(R.string.bundled)
			isSelectable = false
		}
	}
}
