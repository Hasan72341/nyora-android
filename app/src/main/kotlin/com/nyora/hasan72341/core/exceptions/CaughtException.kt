package com.nyora.hasan72341.core.exceptions

class CaughtException(
	override val cause: Throwable
) : RuntimeException("${cause.javaClass.simpleName}(${cause.message})", cause)
