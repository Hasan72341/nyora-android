package com.nyora.hasan72341.core.util.progress

import android.content.Context
import com.google.android.material.slider.LabelFormatter
import com.nyora.hasan72341.R

class IntPercentLabelFormatter(context: Context) : LabelFormatter {

	private val pattern = context.getString(R.string.percent_string_pattern)

	override fun getFormattedValue(value: Float) = pattern.format(value.toInt().toString())
}
