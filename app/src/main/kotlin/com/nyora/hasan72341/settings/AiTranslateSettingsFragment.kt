package com.nyora.hasan72341.settings

import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.ui.BasePreferenceFragment

@AndroidEntryPoint
class AiTranslateSettingsFragment : BasePreferenceFragment(R.string.translate_page) {
	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_ai_translate)
	}
}
