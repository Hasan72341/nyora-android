package com.nyora.hasan72341.mihon.parsers.model

import kotlinx.serialization.Serializable

@Serializable
data class Library(
    val categories: List<FavouriteCategory> = emptyList(),
    val manga: List<Manga> = emptyList(),
)
