package com.nyora.hasan72341.core.exceptions

import okio.IOException
import com.nyora.hasan72341.mihon.parsers.model.MangaSource

abstract class CloudFlareException(
	message: String,
	val state: Int,
) : IOException(message) {

	abstract val url: String

	abstract val source: MangaSource
}
