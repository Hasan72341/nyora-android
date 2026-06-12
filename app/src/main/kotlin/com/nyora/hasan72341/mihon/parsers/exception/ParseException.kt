package com.nyora.hasan72341.mihon.parsers.exception

import com.nyora.hasan72341.mihon.parsers.InternalParsersApi

public class ParseException @InternalParsersApi @JvmOverloads constructor(
	public val shortMessage: String?,
	public val url: String,
	cause: Throwable? = null,
) : RuntimeException("$shortMessage at $url", cause)

