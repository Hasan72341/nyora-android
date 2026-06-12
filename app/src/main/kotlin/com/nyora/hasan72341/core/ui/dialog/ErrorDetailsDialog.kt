package com.nyora.hasan72341.core.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.github.AppUpdateRepository
import com.nyora.hasan72341.core.nav.AppRouter
import com.nyora.hasan72341.core.nav.router
import com.nyora.hasan72341.core.ui.AlertDialogFragment
import com.nyora.hasan72341.core.util.ext.copyToClipboard
import com.nyora.hasan72341.core.util.ext.getCauseUrl
import com.nyora.hasan72341.core.util.ext.isHttpUrl
import com.nyora.hasan72341.core.util.ext.isReportable
import com.nyora.hasan72341.core.util.ext.report
import com.nyora.hasan72341.core.util.ext.requireSerializable
import com.nyora.hasan72341.core.util.ext.setTextAndVisible
import com.nyora.hasan72341.databinding.DialogErrorDetailsBinding
import javax.inject.Inject

@AndroidEntryPoint
class ErrorDetailsDialog : AlertDialogFragment<DialogErrorDetailsBinding>(), View.OnClickListener {

	private lateinit var exception: Throwable

	@Inject
	lateinit var appUpdateRepository: AppUpdateRepository

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val args = requireArguments()
		exception = args.requireSerializable(AppRouter.KEY_ERROR)
	}

	override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup?): DialogErrorDetailsBinding {
		return DialogErrorDetailsBinding.inflate(inflater, container, false)
	}

	override fun onViewBindingCreated(binding: DialogErrorDetailsBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		binding.buttonBrowser.setOnClickListener(this)
		binding.textViewSummary.text = exception.message
		val isUrlAvailable = exception.getCauseUrl()?.isHttpUrl() == true
		binding.buttonBrowser.isVisible = isUrlAvailable
		binding.textViewBrowser.isVisible = isUrlAvailable
		binding.textViewDescription.setTextAndVisible(
			if (appUpdateRepository.isUpdateAvailable) {
				R.string.error_disclaimer_app_outdated
			} else if (exception.isReportable()) {
				R.string.error_disclaimer_report
			} else {
				0
			},
		)
	}

	@Suppress("NAME_SHADOWING")
	override fun onBuildDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
		val builder = super.onBuildDialog(builder)
			.setCancelable(true)
			.setNegativeButton(R.string.close, null)
			.setTitle(R.string.error_details)
			.setNeutralButton(androidx.preference.R.string.copy) { _, _ ->
				context?.copyToClipboard(getString(R.string.error), exception.stackTraceToString())
			}
		if (appUpdateRepository.isUpdateAvailable) {
			builder.setPositiveButton(R.string.update) { _, _ ->
				router.openAppUpdate()
				dismiss()
			}
		} else if (exception.isReportable()) {
			builder.setPositiveButton(R.string.report) { _, _ ->
				exception.report(silent = true)
				dismiss()
			}
		}
		return builder
	}

	override fun onClick(v: View) {
		router.openBrowser(
			url = exception.getCauseUrl() ?: return,
			source = null,
			title = null,
		)
	}
}
