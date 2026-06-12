package com.nyora.hasan72341.mihon.parsers.model

import kotlinx.serialization.Serializable

@Serializable
data class MangaHistory(
    val mangaId: String,
    val chapterId: String,
    val page: Int,
    val scroll: Int,
    val percent: Float,
    val updatedAt: Long,
)
