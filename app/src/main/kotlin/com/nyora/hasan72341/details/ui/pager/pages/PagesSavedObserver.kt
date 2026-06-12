package com.nyora.hasan72341.details.ui.pager.pages

import android.net.Uri
import android.view.View
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.FlowCollector
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.util.ShareHelper

class PagesSavedObserver(
	private val snackbarHost: View,
) : FlowCollector<Collection<Uri>> {

	override suspend fun emit(value: Collection<Uri>) {
		val msg = when (value.size) {
			0 -> R.string.nothing_found
			1 -> R.string.page_saved
			else -> R.string.pages_saved
		}
		val snackbar = Snackbar.make(snackbarHost, msg, Snackbar.LENGTH_LONG)
		value.singleOrNull()?.let { uri ->
			snackbar.setAction(R.string.share) {
				ShareHelper(snackbarHost.context).shareImage(uri)
			}
		}
		snackbar.show()
	}
}
