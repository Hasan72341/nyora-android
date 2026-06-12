package com.nyora.hasan72341.settings.utils

import androidx.preference.EditTextPreference
import androidx.preference.Preference
import com.nyora.hasan72341.R
import com.nyora.hasan72341.mihon.parsers.util.ifNullOrEmpty

class EditTextDefaultSummaryProvider(
	private val defaultValue: String,
) : Preference.SummaryProvider<EditTextPreference> {

	override fun provideSummary(
		preference: EditTextPreference,
	): CharSequence = preference.text.ifNullOrEmpty {
		preference.context.getString(R.string.default_s, defaultValue)
	}
}
