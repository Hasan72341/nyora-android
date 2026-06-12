package com.nyora.hasan72341.mihon.parsers.exception

import com.nyora.hasan72341.mihon.parsers.InternalParsersApi
import com.nyora.hasan72341.mihon.parsers.model.ContentSource
import okio.IOException

/**
 * Authorization is required for access to the requested content
 */
public class AuthRequiredException @InternalParsersApi @JvmOverloads constructor(
	public val source: ContentSource,
	cause: Throwable? = null,
) : IOException("Authorization required", cause)

