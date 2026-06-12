package com.nyora.hasan72341.suggestions.domain

import com.nyora.hasan72341.suggestions.data.AnilistMedia
import com.nyora.hasan72341.mihon.parsers.model.Manga

data class MangaSuggestionV2(
    val anilistMedia: AnilistMedia,
    val matches: List<Manga> = emptyList(),
    val isLoadingMatches: Boolean = false
)
