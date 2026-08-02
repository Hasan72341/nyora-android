package com.nyora.hasan72341.settings

import android.os.Bundle
import android.text.format.DateUtils
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.ui.BasePreferenceFragment
import com.nyora.hasan72341.core.util.ext.getDisplayMessage
import com.nyora.hasan72341.databinding.DialogSyncAuthBinding
import com.nyora.hasan72341.sync.supabase.SupabaseConfig
import com.nyora.hasan72341.sync.supabase.SupabaseSync
import com.nyora.hasan72341.sync.supabase.SupabaseSyncWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

@AndroidEntryPoint
class SyncSettingsFragment : BasePreferenceFragment(R.string.account_and_sync) {

	@Inject
	lateinit var supabaseSync: SupabaseSync

	@Inject
	lateinit var config: SupabaseConfig

	private var authDialog: AlertDialog? = null
	private var isSyncing = false

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_sync)
	}

	override fun onStart() {
		super.onStart()
		refreshState()
	}

	override fun onDestroyView() {
		authDialog?.dismiss()
		authDialog = null
		super.onDestroyView()
	}

	private fun promptSignIn() {
		val binding = DialogSyncAuthBinding.inflate(layoutInflater)
		val dialog = MaterialAlertDialogBuilder(requireContext())
			.setTitle(R.string.sync_auth)
			.setMessage(R.string.sync_auth_hint)
			.setView(binding.root)
			.setPositiveButton(R.string.sign_in, null)
			.setNeutralButton(R.string.create_account, null)
			.setNegativeButton(android.R.string.cancel, null)
			.create()

		dialog.setOnShowListener {
			dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
				submitAuth(dialog, binding, register = false)
			}
			dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
				submitAuth(dialog, binding, register = true)
			}
		}
		dialog.setOnDismissListener {
			if (authDialog === dialog) authDialog = null
		}
		authDialog = dialog
		dialog.show()
	}

	private fun submitAuth(
		dialog: AlertDialog,
		binding: DialogSyncAuthBinding,
		register: Boolean,
	) {
		val email = binding.editEmail.text?.toString()?.trim().orEmpty()
		val password = binding.editPassword.text?.toString().orEmpty()
		binding.layoutEmail.error = null
		binding.layoutPassword.error = null

		if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
			binding.layoutEmail.error = getString(R.string.invalid_email)
			return
		}
		if (password.length < MIN_PASSWORD_LENGTH) {
			binding.layoutPassword.error = getString(R.string.password_length_hint)
			return
		}

		setAuthBusy(dialog, binding, true)
		viewLifecycleOwner.lifecycleScope.launch {
			val authenticated = withContext(Dispatchers.IO) {
				if (register) supabaseSync.register(email, password) else supabaseSync.signIn(email, password)
			}
			if (!authenticated) {
				setAuthBusy(dialog, binding, false)
				binding.layoutPassword.error = getString(
					if (register) R.string.account_creation_failed else R.string.sign_in_failed,
				)
				return@launch
			}

			dialog.dismiss()
			Snackbar.make(listView, R.string.signed_in, Snackbar.LENGTH_SHORT).show()
			SupabaseSyncWorker.schedulePeriodic(requireContext())
			performSync(showStartedMessage = false)
		}
	}

	private fun setAuthBusy(dialog: AlertDialog, binding: DialogSyncAuthBinding, busy: Boolean) {
		binding.progress.isVisible = busy
		binding.editEmail.isEnabled = !busy
		binding.editPassword.isEnabled = !busy
		dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = !busy
		dialog.getButton(AlertDialog.BUTTON_NEUTRAL).isEnabled = !busy
		dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = !busy
	}

	private fun performSync(showStartedMessage: Boolean = true) {
		if (isSyncing || !config.isAuthenticated) return
		isSyncing = true
		refreshState()
		if (showStartedMessage) {
			Snackbar.make(listView, R.string.syncing_library, Snackbar.LENGTH_SHORT).show()
		}

		viewLifecycleOwner.lifecycleScope.launch {
			try {
				withContext(Dispatchers.IO) { supabaseSync.syncNow() }
				SupabaseSyncWorker.schedulePeriodic(requireContext())
				Snackbar.make(listView, R.string.sync_complete, Snackbar.LENGTH_SHORT).show()
			} catch (e: Exception) {
				Snackbar.make(
					listView,
					getString(R.string.sync_failed_message, e.getDisplayMessage(resources)),
					Snackbar.LENGTH_LONG,
				).show()
			} finally {
				isSyncing = false
				refreshState()
			}
		}
	}

	private fun signOut() {
		if (isSyncing) return
		isSyncing = true
		refreshState()
		viewLifecycleOwner.lifecycleScope.launch {
			withContext(Dispatchers.IO) { supabaseSync.signOut() }
			SupabaseSyncWorker.cancel(requireContext())
			isSyncing = false
			refreshState()
			Snackbar.make(listView, R.string.signed_out, Snackbar.LENGTH_SHORT).show()
		}
	}

	private fun refreshState() {
		val signedIn = config.isAuthenticated
		findPreference<Preference>(KEY_STATUS)?.apply {
			title = if (signedIn) config.email.ifBlank { getString(R.string.signed_in) } else getString(R.string.guest)
			summary = getString(if (signedIn) R.string.sync_account_active else R.string.welcome_account_note)
		}

		findPreference<PreferenceCategory>(KEY_GUEST_CATEGORY)?.isVisible = !signedIn
		findPreference<PreferenceCategory>(KEY_ACCOUNT_CATEGORY)?.isVisible = signedIn
		findPreference<Preference>(KEY_SYNC_NOW)?.apply {
			isEnabled = signedIn && !isSyncing
			summary = getString(if (isSyncing) R.string.sync_in_progress else R.string.sync_now_summary)
		}
		findPreference<Preference>(KEY_SIGN_OUT)?.isEnabled = signedIn && !isSyncing

		findPreference<Preference>(KEY_LAST_SYNCED)?.summary = formatLastSynced()
		findPreference<Preference>(KEY_SYNC_ERROR)?.apply {
			isVisible = signedIn && !config.lastSyncError.isNullOrBlank()
			summary = config.lastSyncError
		}
	}

	private fun formatLastSynced(): CharSequence {
		if (config.lastSyncTimestamp == SupabaseConfig.INITIAL_SYNC_TIMESTAMP) {
			return getString(R.string.never)
		}
		return runCatching {
			DateUtils.getRelativeDateTimeString(
				requireContext(),
				Instant.parse(config.lastSyncTimestamp).toEpochMilli(),
				DateUtils.MINUTE_IN_MILLIS,
				DateUtils.WEEK_IN_MILLIS,
				DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME,
			)
		}.getOrDefault(config.lastSyncTimestamp)
	}

	override fun onPreferenceTreeClick(preference: Preference): Boolean {
		return when (preference.key) {
			KEY_SYNC_NOW -> {
				performSync()
				true
			}

			KEY_SIGN_OUT -> {
				signOut()
				true
			}

			KEY_SIGN_IN -> {
				promptSignIn()
				true
			}

			KEY_CONTINUE_GUEST -> {
				parentFragmentManager.popBackStack()
				true
			}

			else -> super.onPreferenceTreeClick(preference)
		}
	}

	private companion object {
		const val MIN_PASSWORD_LENGTH = 4
		const val KEY_STATUS = "supabase_status"
		const val KEY_GUEST_CATEGORY = "supabase_guest_category"
		const val KEY_ACCOUNT_CATEGORY = "supabase_account_category"
		const val KEY_SIGN_IN = "supabase_sign_in"
		const val KEY_CONTINUE_GUEST = "supabase_continue_guest"
		const val KEY_SYNC_NOW = "supabase_sync_now"
		const val KEY_LAST_SYNCED = "supabase_last_synced"
		const val KEY_SYNC_ERROR = "supabase_sync_error"
		const val KEY_SIGN_OUT = "supabase_sign_out"
	}
}
