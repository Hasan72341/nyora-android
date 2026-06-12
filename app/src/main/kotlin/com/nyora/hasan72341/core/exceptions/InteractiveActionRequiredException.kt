package com.nyora.hasan72341.core.exceptions

import okio.IOException
import com.nyora.hasan72341.mihon.parsers.model.MangaSource

class InteractiveActionRequiredException(
	val source: MangaSource,
	val url: String,
) : IOException("Interactive action is required for ${source.name}")
