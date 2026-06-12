package com.nyora.hasan72341.core.exceptions

import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.mihon.parsers.model.MangaSource

class UnsupportedSourceException(
	message: String?,
	val manga: Manga? = null,
	val source: MangaSource? = null,
) : IllegalArgumentException(message)
