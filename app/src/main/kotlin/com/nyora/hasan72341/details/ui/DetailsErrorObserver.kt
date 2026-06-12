package com.nyora.hasan72341.details.ui

import com.google.android.material.snackbar.Snackbar
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.exceptions.UnsupportedSourceException
import com.nyora.hasan72341.core.exceptions.resolve.ErrorObserver
import com.nyora.hasan72341.core.exceptions.resolve.ExceptionResolver
import com.nyora.hasan72341.core.util.ext.getDisplayMessage
import com.nyora.hasan72341.core.util.ext.isNetworkError
import com.nyora.hasan72341.core.util.ext.isSerializable
import com.nyora.hasan72341.mihon.parsers.exception.NotFoundException
import com.nyora.hasan72341.mihon.parsers.exception.ParseException

class DetailsErrorObserver(
	override val activity: DetailsActivity,
	private val viewModel: DetailsViewModel,
	resolver: ExceptionResolver?,
) : ErrorObserver(
	activity.viewBinding.scrollView, null, resolver,
	{ isResolved ->
		if (isResolved) {
			viewModel.reload()
		}
	},
) {

	override suspend fun emit(value: Throwable) {
		val snackbar = Snackbar.make(host, value.getDisplayMessage(host.context.resources), Snackbar.LENGTH_SHORT)
		snackbar.setAnchorView(activity.viewBinding.containerBottomSheet)
		if (value is NotFoundException || value is UnsupportedSourceException) {
			snackbar.duration = Snackbar.LENGTH_INDEFINITE
		}
		when {
			canResolve(value) -> {
				snackbar.setAction(ExceptionResolver.getResolveStringId(value)) {
					resolve(value)
				}
			}

			value is ParseException -> {
				val router = router()
				if (router != null && value.isSerializable()) {
					snackbar.setAction(R.string.details) {
						router.showErrorDialog(value)
					}
				}
			}

			value.isNetworkError() -> {
				snackbar.setAction(R.string.try_again) {
					viewModel.reload()
				}
			}
		}
		snackbar.show()
	}
}
