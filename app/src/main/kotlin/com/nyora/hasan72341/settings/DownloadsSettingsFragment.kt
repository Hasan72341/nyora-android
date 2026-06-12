package com.nyora.hasan72341.settings

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.documentfile.provider.DocumentFile
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.nav.router
import com.nyora.hasan72341.core.os.OpenDocumentTreeHelper
import com.nyora.hasan72341.core.prefs.AppSettings
import com.nyora.hasan72341.core.prefs.DownloadFormat
import com.nyora.hasan72341.core.prefs.TriStateOption
import com.nyora.hasan72341.core.ui.BasePreferenceFragment
import com.nyora.hasan72341.core.util.ext.getQuantityStringSafe
import com.nyora.hasan72341.core.util.ext.printStackTraceDebug
import com.nyora.hasan72341.core.util.ext.resolveFile
import com.nyora.hasan72341.core.util.ext.setDefaultValueCompat
import com.nyora.hasan72341.core.util.ext.tryLaunch
import com.nyora.hasan72341.core.util.ext.viewLifecycleScope
import com.nyora.hasan72341.download.ui.worker.DownloadWorker
import com.nyora.hasan72341.local.data.LocalStorageManager
import com.nyora.hasan72341.mihon.parsers.util.names
import com.nyora.hasan72341.settings.utils.DozeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class DownloadsSettingsFragment :
	BasePreferenceFragment(R.string.downloads),
	SharedPreferences.OnSharedPreferenceChangeListener {

	private val dozeHelper = DozeHelper(this)

	@Inject
	lateinit var storageManager: LocalStorageManager

	@Inject
	lateinit var downloadsScheduler: DownloadWorker.Scheduler

	private val pickFileTreeLauncher = OpenDocumentTreeHelper(this) {
		if (it != null) onDirectoryPicked(it)
	}

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_downloads)
		findPreference<ListPreference>(AppSettings.KEY_DOWNLOADS_FORMAT)?.run {
			entryValues = DownloadFormat.entries.names()
			setDefaultValueCompat(DownloadFormat.AUTOMATIC.name)
		}
		findPreference<ListPreference>(AppSettings.KEY_DOWNLOADS_METERED_NETWORK)?.run {
			entryValues = TriStateOption.entries.names()
			setDefaultValueCompat(TriStateOption.ASK.name)
		}
		dozeHelper.updatePreference()
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		findPreference<Preference>(AppSettings.KEY_LOCAL_STORAGE)?.bindStorageName()
		findPreference<Preference>(AppSettings.KEY_LOCAL_MANGA_DIRS)?.bindDirectoriesCount()
		findPreference<Preference>(AppSettings.KEY_PAGES_SAVE_DIR)?.bindPagesDirectory()
		settings.subscribe(this)
	}

	override fun onDestroyView() {
		settings.unsubscribe(this)
		super.onDestroyView()
	}

	override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
		when (key) {
			AppSettings.KEY_LOCAL_STORAGE -> {
				findPreference<Preference>(key)?.bindStorageName()
			}

			AppSettings.KEY_LOCAL_MANGA_DIRS -> {
				findPreference<Preference>(key)?.bindDirectoriesCount()
			}

			AppSettings.KEY_DOWNLOADS_METERED_NETWORK -> {
				updateDownloadsConstraints()
			}

			AppSettings.KEY_PAGES_SAVE_DIR -> {
				findPreference<Preference>(AppSettings.KEY_PAGES_SAVE_DIR)?.bindPagesDirectory()
			}
		}
	}

	override fun onPreferenceTreeClick(preference: Preference): Boolean {
		return when (preference.key) {
			AppSettings.KEY_LOCAL_STORAGE -> {
				router.showDirectorySelectDialog()
				true
			}

			AppSettings.KEY_LOCAL_MANGA_DIRS -> {
				router.openDirectoriesSettings()
				true
			}

			AppSettings.KEY_IGNORE_DOZE -> {
				dozeHelper.startIgnoreDoseActivity()
			}

			AppSettings.KEY_PAGES_SAVE_DIR -> {
				if (!pickFileTreeLauncher.tryLaunch(settings.getPagesSaveDir(preference.context)?.uri)) {
					Snackbar.make(
						requireView(), R.string.operation_not_supported, Snackbar.LENGTH_SHORT,
					).show()
				}
				true
			}

			else -> super.onPreferenceTreeClick(preference)
		}
	}

	private fun onDirectoryPicked(uri: Uri) {
		storageManager.takePermissions(uri)
		val doc = DocumentFile.fromTreeUri(requireContext(), uri)?.takeIf {
			it.canWrite()
		}
		settings.setPagesSaveDir(doc?.uri)
	}

	private fun Preference.bindStorageName() {
		viewLifecycleScope.launch {
			val storage = storageManager.getDefaultWriteableDir()
			summary = if (storage != null) {
				storageManager.getDirectoryDisplayName(storage, isFullPath = true)
			} else {
				getString(R.string.not_available)
			}
		}
	}

	private fun Preference.bindDirectoriesCount() {
		viewLifecycleScope.launch {
			val dirs = storageManager.getReadableDirs().size
			summary = resources.getQuantityStringSafe(R.plurals.items, dirs, dirs)
		}
	}

	private fun Preference.bindPagesDirectory() {
		viewLifecycleScope.launch {
			val df = withContext(Dispatchers.IO) {
				settings.getPagesSaveDir(this@bindPagesDirectory.context)
			}
			summary = df?.getDisplayPath(this@bindPagesDirectory.context)
				?: this@bindPagesDirectory.context.getString(androidx.preference.R.string.not_set)
		}
	}

	private fun updateDownloadsConstraints() {
		val preference = findPreference<Preference>(AppSettings.KEY_DOWNLOADS_METERED_NETWORK)
		viewLifecycleScope.launch {
			try {
				preference?.isEnabled = false
				withContext(Dispatchers.IO) {
					val option = when (settings.allowDownloadOnMeteredNetwork) {
						TriStateOption.ENABLED -> true
						TriStateOption.ASK -> return@withContext
						TriStateOption.DISABLED -> false
					}
					downloadsScheduler.updateConstraints(option)
				}
			} catch (e: Exception) {
				e.printStackTraceDebug("DownloadsSettingsFragment::updateDownloadsConstraints")
			} finally {
				preference?.isEnabled = true
			}
		}
	}

	private fun DocumentFile.getDisplayPath(context: Context): String {
		return uri.resolveFile(context)?.path ?: uri.toString()
	}

}
