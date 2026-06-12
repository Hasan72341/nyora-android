package com.nyora.hasan72341.suggestions.domain

import androidx.annotation.FloatRange
import com.nyora.hasan72341.mihon.parsers.model.Manga

data class MangaSuggestion(
	val manga: Manga,
	@FloatRange(from = 0.0, to = 1.0)
	val relevance: Float,
)