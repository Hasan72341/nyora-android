package com.nyora.hasan72341.mihon.parsers.model

import kotlinx.serialization.Serializable

@Serializable
data class FavouriteCategory(
    val id: Long,
    val title: String,
    val order: Int = 0,
    val showInLib: Boolean = true,
    val track: Boolean = false,
)
