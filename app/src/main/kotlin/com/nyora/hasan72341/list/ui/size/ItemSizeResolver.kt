package com.nyora.hasan72341.list.ui.size

import android.view.View
import android.widget.TextView
import com.nyora.hasan72341.history.ui.util.ReadingProgressView

interface ItemSizeResolver {

	val cellWidth: Int

	fun attachToView(
		view: View,
		textView: TextView?,
		progressView: ReadingProgressView?,
	)
}
