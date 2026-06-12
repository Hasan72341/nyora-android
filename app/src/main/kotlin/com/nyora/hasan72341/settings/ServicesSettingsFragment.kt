package com.nyora.hasan72341.settings

import android.accounts.AccountManager
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.nav.router
import com.nyora.hasan72341.core.prefs.AppSettings
import com.nyora.hasan72341.core.ui.BasePreferenceFragment
import com.nyora.hasan72341.core.ui.dialog.buildAlertDialog
import com.nyora.hasan72341.core.util.ext.getDisplayMessage
import com.nyora.hasan72341.core.util.ext.printStackTraceDebug
import com.nyora.hasan72341.core.util.ext.viewLifecycleScope
import com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblerService
import com.nyora.hasan72341.scrobbling.common.ui.ScrobblerAuthHelper
import com.nyora.hasan72341.settings.utils.SplitSwitchPreference

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class ServicesSettingsFragment : BasePreferenceFragment(R.string.services),
	SharedPreferences.OnSharedPreferenceChangeListener {

	@Inject
	lateinit var scrobblerAuthHelper: ScrobblerAuthHelper

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_services)
		findPreference<SplitSwitchPreference>(AppSettings.KEY_STATS_ENABLED)?.let {
			it.onContainerClickListener = Preference.OnPreferenceClickListener {
				router.openStatistic()
				true
			}
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		bindSuggestionsSummary()
		bindStatsSummary()
		bindAiTranslateSummary()
		settings.subscribe(this)
	}

	override fun onDestroyView() {
		settings.unsubscribe(this)
		super.onDestroyView()
	}

	override fun onResume() {
		super.onResume()
		bindScrobblerSummary(AppSettings.KEY_SHIKIMORI, ScrobblerService.SHIKIMORI)
		bindScrobblerSummary(AppSettings.KEY_ANILIST, ScrobblerService.ANILIST)
		bindScrobblerSummary(AppSettings.KEY_MAL, ScrobblerService.MAL)
		bindScrobblerSummary(AppSettings.KEY_KITSU, ScrobblerService.KITSU)
		bindSyncSummary()
		bindAiTranslateSummary()
	}

	override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
		when (key) {
			AppSettings.KEY_SUGGESTIONS -> bindSuggestionsSummary()
			AppSettings.KEY_STATS_ENABLED -> bindStatsSummary()
			AppSettings.KEY_AI_TRANSLATE_ENABLED -> bindAiTranslateSummary()
		}
	}


	override fun onPreferenceTreeClick(preference: Preference): Boolean {
		return when (preference.key) {
			AppSettings.KEY_SHIKIMORI -> {
				handleScrobblerClick(ScrobblerService.SHIKIMORI)
				true
			}

			AppSettings.KEY_MAL -> {
				handleScrobblerClick(ScrobblerService.MAL)
				true
			}

			AppSettings.KEY_ANILIST -> {
				handleScrobblerClick(ScrobblerService.ANILIST)
				true
			}

			AppSettings.KEY_KITSU -> {
				handleScrobblerClick(ScrobblerService.KITSU)
				true
			}

			AppSettings.KEY_SYNC -> {
				// Simply open the SyncSettingsFragment which now handles Supabase
				true
			}

			else -> super.onPreferenceTreeClick(preference)
		}
	}

	private fun bindScrobblerSummary(
		key: String,
		scrobblerService: ScrobblerService
	) {
		val pref = findPreference<Preference>(key) ?: return
		if (!scrobblerAuthHelper.isAuthorized(scrobblerService)) {
			pref.setSummary(R.string.disabled)
			return
		}
		val username = scrobblerAuthHelper.getCachedUser(scrobblerService)?.nickname
		if (username != null) {
			pref.summary = getString(R.string.logged_in_as, username)
		} else {
			pref.setSummary(R.string.loading_)
			viewLifecycleScope.launch {
				pref.summary = withContext(Dispatchers.IO) {
					runCatching {
						val user = scrobblerAuthHelper.getUser(scrobblerService)
						getString(R.string.logged_in_as, user.nickname)
					}.getOrElse {
						it.printStackTraceDebug("ServicesSettingsFragment::bindScrobblerSummary")
						it.getDisplayMessage(resources)
					}
				}
			}
		}
	}

	private fun handleScrobblerClick(scrobblerService: ScrobblerService) {
		if (!scrobblerAuthHelper.isAuthorized(scrobblerService)) {
			confirmScrobblerAuth(scrobblerService)
		} else {
			router.openScrobblerSettings(scrobblerService)
		}
	}

	private fun bindSyncSummary() {
		// Summary can just be a static string or reflect Supabase auth state if desired.
		findPreference<Preference>(AppSettings.KEY_SYNC)?.summary = "Supabase Sync"
		findPreference<Preference>(AppSettings.KEY_SYNC_SETTINGS)?.isEnabled = true
	}

	private fun bindSuggestionsSummary() {
		findPreference<Preference>(AppSettings.KEY_SUGGESTIONS)?.setSummary(
			if (settings.isSuggestionsEnabled) R.string.enabled else R.string.disabled,
		)
	}

	private fun bindStatsSummary() {
		findPreference<Preference>(AppSettings.KEY_STATS_ENABLED)?.setSummary(
			if (settings.isStatsEnabled) R.string.enabled else R.string.disabled,
		)
	}

	private fun bindAiTranslateSummary() {
		findPreference<Preference>("ai_translate")?.setSummary(
			if (settings.isAiTranslateEnabled) R.string.enabled else R.string.disabled,
		)
	}

	private fun confirmScrobblerAuth(scrobblerService: ScrobblerService) {
		buildAlertDialog(context ?: return, isCentered = true) {
			setIcon(scrobblerService.iconResId)
			setTitle(scrobblerService.titleResId)
			setMessage(context.getString(R.string.scrobbler_auth_intro, context.getString(scrobblerService.titleResId)))
			setPositiveButton(R.string.sign_in) { _, _ ->
				scrobblerAuthHelper.startAuth(context, scrobblerService).onFailure {
					Snackbar.make(listView, it.getDisplayMessage(resources), Snackbar.LENGTH_LONG).show()
				}
			}
			setNegativeButton(android.R.string.cancel, null)
		}.show()
	}
}
