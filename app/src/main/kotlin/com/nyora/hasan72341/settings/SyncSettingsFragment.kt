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

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_sync)
	}

	override fun onStart() {
		super.onStart()
		refreshState()
	}

	/** Email/password sign-in (or registration) dialog against the self-hosted server. */
	private fun promptSignIn() {
		val ctx = requireContext()
		val pad = (16 * resources.displayMetrics.density).toInt()
		val emailInput = android.widget.EditText(ctx).apply {
			hint = getString(R.string.email)
			inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
		}
		val passwordInput = android.widget.EditText(ctx).apply {
			hint = getString(R.string.password)
			inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
		}
		val container = android.widget.LinearLayout(ctx).apply {
			orientation = android.widget.LinearLayout.VERTICAL
			setPadding(pad, pad / 2, pad, 0)
			addView(emailInput)
			addView(passwordInput)
		}
		com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
			.setTitle(R.string.sign_in)
			.setView(container)
			.setPositiveButton(R.string.sign_in) { _, _ ->
				doAuth(emailInput.text.toString(), passwordInput.text.toString(), register = false)
			}
			.setNeutralButton(R.string.create_account) { _, _ ->
				doAuth(emailInput.text.toString(), passwordInput.text.toString(), register = true)
			}
			.setNegativeButton(android.R.string.cancel, null)
			.show()
	}

	private fun doAuth(email: String, password: String, register: Boolean) {
		val e = email.trim()
		if (e.isEmpty() || password.isEmpty()) {
			view?.let { Snackbar.make(it, R.string.enter_email_and_password, Snackbar.LENGTH_SHORT).show() }
			return
		}
		lifecycleScope.launch(Dispatchers.IO) {
			val ok = if (register) supabaseSync.register(e, password) else supabaseSync.signIn(e, password)
			withContext(Dispatchers.Main) {
				val v = view ?: return@withContext
				if (ok) {
					Snackbar.make(v, R.string.signed_in, Snackbar.LENGTH_SHORT).show()
					SupabaseSyncWorker.schedulePeriodic(requireContext())
					checkFirstLoginMerge()
					refreshState()
				} else {
					Snackbar.make(v, R.string.sign_in_failed, Snackbar.LENGTH_LONG).show()
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
				promptSignIn()
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