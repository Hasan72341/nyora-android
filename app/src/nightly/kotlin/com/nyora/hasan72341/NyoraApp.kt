package com.nyora.hasan72341

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import leakcanary.LeakCanary
import com.nyora.hasan72341.core.BaseApp

class NyoraApp : BaseApp(), SharedPreferences.OnSharedPreferenceChangeListener {

	override fun attachBaseContext(base: Context) {
		super.attachBaseContext(base)
		val prefs = PreferenceManager.getDefaultSharedPreferences(this)
		configureLeakCanary(isEnabled = prefs.getBoolean(KEY_LEAK_CANARY, false))
		prefs.registerOnSharedPreferenceChangeListener(this)
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
		if (key == KEY_LEAK_CANARY) {
			configureLeakCanary(sharedPreferences.getBoolean(KEY_LEAK_CANARY, false))
		}
	}

	private fun configureLeakCanary(isEnabled: Boolean) {
		LeakCanary.config = LeakCanary.config.copy(
			dumpHeap = isEnabled,
		)
	}

	private companion object {

		const val KEY_LEAK_CANARY = "debug.leak_canary"
	}
}
