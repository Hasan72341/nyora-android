package com.nyora.hasan72341.settings

import android.os.Bundle
import androidx.preference.Preference
import leakcanary.LeakCanary
import com.nyora.hasan72341.NyoraApp
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.model.TestMangaSource
import com.nyora.hasan72341.core.nav.router
import com.nyora.hasan72341.core.ui.BasePreferenceFragment
import com.nyora.hasan72341.settings.utils.SplitSwitchPreference
import org.koitharu.workinspector.WorkInspector

class DebugSettingsFragment : BasePreferenceFragment(R.string.debug), Preference.OnPreferenceChangeListener,
	Preference.OnPreferenceClickListener {

	private val application
		get() = requireContext().applicationContext as NyoraApp

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_debug)
		findPreference<SplitSwitchPreference>(KEY_LEAK_CANARY)?.let { pref ->
			pref.isChecked = application.isLeakCanaryEnabled
			pref.onPreferenceChangeListener = this
			pref.onContainerClickListener = this
		}
	}

	override fun onResume() {
		super.onResume()
		findPreference<SplitSwitchPreference>(KEY_LEAK_CANARY)?.isChecked = application.isLeakCanaryEnabled
	}

	override fun onPreferenceTreeClick(preference: Preference): Boolean = when (preference.key) {
		KEY_WORK_INSPECTOR -> {
			startActivity(WorkInspector.getIntent(preference.context))
			true
		}

		KEY_TEST_PARSER -> {
			router.openList(TestMangaSource, null, null)
			true
		}

		else -> super.onPreferenceTreeClick(preference)
	}

	override fun onPreferenceClick(preference: Preference): Boolean = when (preference.key) {
		KEY_LEAK_CANARY -> {
			startActivity(LeakCanary.newLeakDisplayActivityIntent())
			true
		}

		else -> super.onPreferenceTreeClick(preference)
	}

	override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean = when (preference.key) {
		KEY_LEAK_CANARY -> {
			application.isLeakCanaryEnabled = newValue as Boolean
			true
		}

		else -> false
	}

	private companion object {

		const val KEY_LEAK_CANARY = "leak_canary"
		const val KEY_WORK_INSPECTOR = "work_inspector"
		const val KEY_TEST_PARSER = "test_parser"
	}
}
