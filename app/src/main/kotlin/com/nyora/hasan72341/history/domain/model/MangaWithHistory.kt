package com.nyora.hasan72341.history.domain.model

import com.nyora.hasan72341.core.model.MangaHistory
import com.nyora.hasan72341.mihon.parsers.model.Manga

data class MangaWithHistory(
	val manga: Manga,
	val history: MangaHistory
)
