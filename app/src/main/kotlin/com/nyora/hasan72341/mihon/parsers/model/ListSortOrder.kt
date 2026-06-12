package com.nyora.hasan72341.mihon.parsers.model

import kotlinx.serialization.Serializable

@Serializable
enum class SortDirection { ASC, DESC }

@Serializable
enum class ListSortOrder {
    UPDATED, RATING, POPULARITY, DATE, NAME, NEWEST, ALPHABETICAL,
}
