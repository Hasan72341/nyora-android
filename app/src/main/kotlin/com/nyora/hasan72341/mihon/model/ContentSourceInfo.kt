package com.nyora.hasan72341.mihon.model

import com.nyora.hasan72341.mihon.parsers.model.ContentSource

data class ContentSourceInfo(
    val mangaSource: ContentSource,
    val isEnabled: Boolean,
    val isPinned: Boolean,
) : ContentSource by mangaSource
