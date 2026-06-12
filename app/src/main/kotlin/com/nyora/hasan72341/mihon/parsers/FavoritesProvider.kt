package com.nyora.hasan72341.mihon.parsers

import com.nyora.hasan72341.mihon.parsers.model.Content

interface FavoritesProvider {

    suspend fun fetchFavorites(): List<Content>
}
