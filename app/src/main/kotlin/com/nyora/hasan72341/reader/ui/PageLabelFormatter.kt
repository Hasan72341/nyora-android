package com.nyora.hasan72341.reader.ui

import com.google.android.material.slider.LabelFormatter
import com.nyora.hasan72341.mihon.parsers.util.format

class PageLabelFormatter : LabelFormatter {

	override fun getFormattedValue(value: Float): String {
		return (value + 1).format(0)
	}
}
