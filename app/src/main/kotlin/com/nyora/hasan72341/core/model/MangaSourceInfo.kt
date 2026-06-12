package com.nyora.hasan72341.core.model

import com.nyora.hasan72341.mihon.parsers.model.MangaSource

data class MangaSourceInfo(
	val mangaSource: MangaSource,
	val isEnabled: Boolean,
	val isPinned: Boolean,
) : MangaSource by mangaSource
