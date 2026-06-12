package com.nyora.hasan72341.mihon.parsers

import com.nyora.hasan72341.mihon.parsers.model.Content

interface FavoritesSyncProvider {

    suspend fun addFavorite(manga: Content): Boolean

    suspend fun removeFavorite(manga: Content): Boolean
}
