package com.nyora.hasan72341.settings.utils

import androidx.annotation.StringRes
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import com.nyora.hasan72341.mihon.parsers.util.ifNullOrEmpty

class EditTextFallbackSummaryProvider(
	@StringRes private val fallbackResId: Int,
) : Preference.SummaryProvider<EditTextPreference> {

	override fun provideSummary(
		preference: EditTextPreference,
	): CharSequence = preference.text.ifNullOrEmpty {
		preference.context.getString(fallbackResId)
	}
}
