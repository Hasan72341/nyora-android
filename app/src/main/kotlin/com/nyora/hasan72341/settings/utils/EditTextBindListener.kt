package com.nyora.hasan72341.settings.utils

import android.widget.EditText
import androidx.preference.EditTextPreference
import com.nyora.hasan72341.core.util.EditTextValidator

class EditTextBindListener(
	private val inputType: Int,
	private val hint: String?,
	private val validator: EditTextValidator?,
) : EditTextPreference.OnBindEditTextListener {

	override fun onBindEditText(editText: EditText) {
		editText.inputType = inputType
		editText.hint = hint
		validator?.attachToEditText(editText)
	}
}
