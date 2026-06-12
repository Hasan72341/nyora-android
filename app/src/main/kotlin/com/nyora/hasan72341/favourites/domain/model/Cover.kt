package com.nyora.hasan72341.favourites.domain.model

import com.nyora.hasan72341.core.model.MangaSource

data class Cover(
	val url: String?,
	val source: String,
) {
	val mangaSource by lazy { MangaSource(source) }
}
