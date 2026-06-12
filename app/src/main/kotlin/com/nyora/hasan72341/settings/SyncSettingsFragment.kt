package com.nyora.hasan72341.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.db.MangaDatabase
import com.nyora.hasan72341.core.ui.BasePreferenceFragment
import com.nyora.hasan72341.sync.supabase.SupabaseSync
import com.nyora.hasan72341.sync.supabase.SupabaseConfig
import com.nyora.hasan72341.sync.supabase.SupabaseGoogleAuthHelper
import com.nyora.hasan72341.sync.supabase.SupabaseSyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

private const val RC_GOOGLE_SIGN_IN = 9001

@AndroidEntryPoint
class SyncSettingsFragment : BasePreferenceFragment(R.string.sync_settings) {

	@Inject
	lateinit var supabaseSync: SupabaseSync

	@Inject
	lateinit var config: SupabaseConfig

	@Inject
	lateinit var database: MangaDatabase

	private var googleSignInLauncher: androidx.activity.result.ActivityResultLauncher<Intent>? = null

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_sync)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		registerGoogleSignInLauncher()
	}

	override fun onStart() {
		super.onStart()
		refreshState()
	}

	private fun registerGoogleSignInLauncher() {
		googleSignInLauncher = registerForActivityResult(
			androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
		) { result ->
			when (val signInResult = SupabaseGoogleAuthHelper.handleResult(result.data)) {
				is SupabaseGoogleAuthHelper.GoogleSignInResult.Success -> {
					lifecycleScope.launch(Dispatchers.IO) {
						val ok = supabaseSync.signInWithGoogle(signInResult.idToken)
						withContext(Dispatchers.Main) {
							if (ok) {
								Snackbar.make(view!!, "Signed in", Snackbar.LENGTH_SHORT).show()
								SupabaseSyncWorker.schedulePeriodic(requireContext())
								checkFirstLoginMerge()
								refreshState()
							} else {
								Snackbar.make(view!!, "Sign-in failed", Snackbar.LENGTH_LONG).show()
							}
						}
					}
				}
				is SupabaseGoogleAuthHelper.GoogleSignInResult.Error -> {
					view?.let { Snackbar.make(it, signInResult.message, Snackbar.LENGTH_LONG).show() }
				}
			}
		}
	}

	private fun checkFirstLoginMerge() {
		// Check if local data exists and first-login flag not set
		if (config.firstLoginHandled) return
		lifecycleScope.launch(Dispatchers.IO) {
			val hasLocalData = hasLocalSyncableData()
			if (hasLocalData) {
				withContext(Dispatchers.Main) {
					showMergeDialog()
				}
			} else {
				config.firstLoginHandled = true
				config.saveTokens()
				supabaseSync.syncNow()
				SupabaseSyncWorker.schedulePeriodic(requireContext())
			}
		}
	}

	private suspend fun hasLocalSyncableData(): Boolean {
		if (database.getFavouritesDao().findAll().isNotEmpty()) return true
		if (database.getHistoryDao().findAll(0, 1).isNotEmpty()) return true
		if (database.getBookmarksDao().findAll(0, 1).isNotEmpty()) return true
		if (database.getFavouriteCategoriesDao().findAll().isNotEmpty()) return true
		if (database.getPreferencesDao().findAll().isNotEmpty()) return true
		if (database.getExternalExtensionRepoDao().findAll().isNotEmpty()) return true
		val sources = database.getSourcesDao().findAll()
		if (sources.any { it.isPinned || !it.isEnabled || it.lastUsedAt > 0 }) return true
		return false
	}

	private fun showMergeDialog() {
		val items = arrayOf(
			"Merge local data into account",
			"Replace local with cloud data",
			"Keep guest data separate"
		)
		com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
			.setTitle("Sync your data?")
			.setMessage("Local data found. What would you like to do?")
			.setItems(items) { _, which ->
				lifecycleScope.launch(Dispatchers.IO) {
					config.firstLoginHandled = true
					config.saveTokens()
					when (which) {
						0 -> { // Merge
							supabaseSync.syncNow()
						}
						1 -> { // Replace local with cloud
							config.resetSyncCursor()
							supabaseSync.restoreFromCloud()
						}
						2 -> { // Keep separate — do nothing
						}
					}
					SupabaseSyncWorker.schedulePeriodic(requireContext())
					withContext(Dispatchers.Main) {
						refreshState()
					}
				}
			}
			.setCancelable(false)
			.show()
	}

	private fun refreshState() {
		val signedIn = config.isAuthenticated

		// Status
		findPreference<Preference>("supabase_status")?.apply {
			if (signedIn) {
				title = config.email.ifBlank { "Signed In" }
				summary = config.userId
			} else {
				title = "Guest"
				summary = "Your data stays tied to your account. You can also continue as a guest and sync later."
			}
		}

		// Guest category visibility
		findPreference<PreferenceCategory>("supabase_guest_category")?.isVisible = !signedIn

		// Account category visibility
		findPreference<PreferenceCategory>("supabase_account_category")?.isVisible = signedIn

		// Last synced
		findPreference<Preference>("supabase_last_synced")?.apply {
			if (signedIn && config.lastSyncTimestamp != SupabaseConfig.INITIAL_SYNC_TIMESTAMP) {
				summary = try {
					java.time.OffsetDateTime.parse(config.lastSyncTimestamp).toInstant()
						.atZone(ZoneId.systemDefault())
						.toLocalDateTime()
						.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
				} catch (e: Exception) {
					config.lastSyncTimestamp
				}
			} else {
				summary = "Never"
			}
		}

		// Sync error (clear on refresh)
		findPreference<Preference>("supabase_sync_error")?.isVisible = false
	}

	override fun onPreferenceTreeClick(preference: Preference): Boolean {
		return when (preference.key) {
			"supabase_sync_now" -> {
				val v = view
				if (v != null && config.isAuthenticated) {
					Snackbar.make(v, "Starting Supabase Sync...", Snackbar.LENGTH_SHORT).show()
					lifecycleScope.launch(Dispatchers.IO) {
						try {
							supabaseSync.syncNow()
							SupabaseSyncWorker.schedulePeriodic(requireContext())
							withContext(Dispatchers.Main) {
								Snackbar.make(v, "Sync Complete!", Snackbar.LENGTH_SHORT).show()
								refreshState()
							}
						} catch (e: Exception) {
							withContext(Dispatchers.Main) {
								findPreference<Preference>("supabase_sync_error")?.apply {
									isVisible = true
									summary = e.message ?: "Sync failed"
								}
								Snackbar.make(v, "Sync failed: ${e.message}", Snackbar.LENGTH_LONG).show()
							}
						}
					}
				}
				true
			}
			"supabase_sign_out" -> {
				supabaseSync.signOut()
				SupabaseSyncWorker.cancel(requireContext())
				view?.let { Snackbar.make(it, "Signed out", Snackbar.LENGTH_SHORT).show() }
				refreshState()
				true
			}
			"supabase_sign_in_google" -> {
				val intent = SupabaseGoogleAuthHelper.createIntent(requireContext())
				googleSignInLauncher?.launch(intent)
				true
			}
			"supabase_continue_guest" -> {
				view?.let { Snackbar.make(it, "Continuing as guest", Snackbar.LENGTH_SHORT).show() }
				true
			}
			else -> super.onPreferenceTreeClick(preference)
		}
	}
}