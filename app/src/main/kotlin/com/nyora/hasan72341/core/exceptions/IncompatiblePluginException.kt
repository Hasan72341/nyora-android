package com.nyora.hasan72341.core.exceptions

class IncompatiblePluginException(
	val name: String?,
	cause: Throwable?,
) : RuntimeException(cause)
