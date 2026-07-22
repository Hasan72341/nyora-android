package com.nyora.hasan72341.settings

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.Formatter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import dagger.hilt.android.AndroidEntryPoint
import com.nyora.hasan72341.R
import com.nyora.hasan72341.ai.colorize.ColorizeModelManager
import com.nyora.hasan72341.ai.colorize.MangaColorizer
import com.nyora.hasan72341.core.prefs.AppSettings
import com.nyora.hasan72341.core.ui.BasePreferenceFragment
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Settings for on-device manga colorization. Mirrors the web app's Colorization sub-page: the model
 * (~62 MB) is downloaded EXPLICITLY here with a progress indicator, the Colorize toggle stays locked
 * until it's present, and the model can be deleted to reclaim storage. See [ColorizeModelManager].
 */
@AndroidEntryPoint
class AiColorizeSettingsFragment : BasePreferenceFragment(R.string.colorize_page) {

	private val mainHandler = Handler(Looper.getMainLooper())
	private var downloadJob: Job? = null

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_ai_colorize)

		findPreference<Preference>("colorize_model")?.onPreferenceClickListener =
			Preference.OnPreferenceClickListener {
				startDownload()
				true
			}
		findPreference<Preference>("colorize_delete")?.onPreferenceClickListener =
			Preference.OnPreferenceClickListener {
				deleteModel()
				true
			}
	}

	override fun onResume() {
		super.onResume()
		refreshState()
	}

	override fun onDestroyView() {
		mainHandler.removeCallbacksAndMessages(null)
		super.onDestroyView()
	}

	/** Reflect model presence + device support into the toggle and the model/delete rows. */
	private fun refreshState() {
		val ctx = context ?: return
		val enabledPref = findPreference<SwitchPreferenceCompat>(AppSettings.KEY_COLORIZE_ENABLED)
		val modelPref = findPreference<Preference>("colorize_model")
		val deletePref = findPreference<Preference>("colorize_delete")

		if (!ColorizeModelManager.isDeviceSupported(ctx)) {
			enabledPref?.isEnabled = false
			enabledPref?.isChecked = false
			enabledPref?.setSummary(R.string.colorize_unsupported_device)
			modelPref?.isEnabled = false
			modelPref?.setSummary(R.string.colorize_unsupported_device)
			deletePref?.isVisible = false
			return
		}

		val downloaded = ColorizeModelManager.isDownloaded(ctx)
		enabledPref?.isEnabled = downloaded
		enabledPref?.setSummary(
			if (downloaded) R.string.colorize_enabled_summary else R.string.colorize_needs_model,
		)
		if (!downloaded) enabledPref?.isChecked = false

		if (downloaded) {
			val size = Formatter.formatShortFileSize(ctx, ColorizeModelManager.sizeOnDisk(ctx))
			modelPref?.summary = getString(R.string.colorize_model_ready, size)
			deletePref?.isVisible = true
		} else {
			modelPref?.setSummary(R.string.colorize_model_not_downloaded)
			deletePref?.isVisible = false
		}
	}

	private fun startDownload() {
		val ctx = context ?: return
		if (downloadJob?.isActive == true) return
		if (ColorizeModelManager.isDownloaded(ctx)) {
			refreshState()
			return
		}
		val modelPref = findPreference<Preference>("colorize_model")
		modelPref?.isEnabled = false
		modelPref?.setSummary(R.string.colorize_downloading_start)

		downloadJob = viewLifecycleOwner.lifecycleScope.launch {
			try {
				ColorizeModelManager.download(ctx.applicationContext) { pct ->
					mainHandler.post {
						modelPref?.summary = getString(R.string.colorize_downloading, pct)
					}
				}
				Toast.makeText(ctx, R.string.colorize_model_downloaded, Toast.LENGTH_SHORT).show()
			} catch (e: Throwable) {
				val msg = e.message ?: getString(R.string.colorize_download_failed)
				modelPref?.summary = getString(R.string.colorize_download_failed_fmt, msg)
				Toast.makeText(ctx, R.string.colorize_download_failed, Toast.LENGTH_LONG).show()
			} finally {
				modelPref?.isEnabled = true
				refreshState()
			}
		}
	}

	private fun deleteModel() {
		val ctx = context ?: return
		MangaColorizer.close()
		ColorizeModelManager.delete(ctx)
		settings.isColorizeEnabled = false
		Toast.makeText(ctx, R.string.colorize_deleted, Toast.LENGTH_SHORT).show()
		refreshState()
	}
}
