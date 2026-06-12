package com.nyora.hasan72341.core.exceptions

import com.nyora.hasan72341.details.ui.pager.EmptyMangaReason
import com.nyora.hasan72341.mihon.parsers.model.Manga

class EmptyMangaException(
    val reason: EmptyMangaReason?,
    val manga: Manga,
    cause: Throwable?
) : IllegalStateException(cause)
