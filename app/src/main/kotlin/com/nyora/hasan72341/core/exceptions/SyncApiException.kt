package com.nyora.hasan72341.core.exceptions

class SyncApiException(
	message: String,
	val code: Int,
) : RuntimeException(message)
