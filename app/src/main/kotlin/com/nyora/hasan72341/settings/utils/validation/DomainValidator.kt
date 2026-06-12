package com.nyora.hasan72341.settings.utils.validation

import okhttp3.HttpUrl
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.util.EditTextValidator

class DomainValidator : EditTextValidator() {

	override fun validate(text: String): ValidationResult {
		val trimmed = text.trim()
		if (trimmed.isEmpty()) {
			return ValidationResult.Success
		}
		return if (!isValidDomain(trimmed)) {
			ValidationResult.Failed(context.getString(R.string.invalid_domain_message))
		} else {
			ValidationResult.Success
		}
	}

	companion object {

		fun isValidDomain(value: String): Boolean = runCatching {
			require(value.isNotEmpty())
			val parts = value.split(':')
			require(parts.size <= 2)
			val urlBuilder = HttpUrl.Builder()
			urlBuilder.host(parts.first())
			if (parts.size == 2) {
				urlBuilder.port(parts[1].toInt())
			}
		}.isSuccess
	}
}
