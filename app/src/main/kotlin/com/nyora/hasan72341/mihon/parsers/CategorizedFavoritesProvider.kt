package com.nyora.hasan72341.mihon.parsers

import com.nyora.hasan72341.mihon.parsers.model.Content

interface CategorizedFavoritesProvider : FavoritesProvider {

    suspend fun fetchFavoriteFolders(): List<ContentFavoriteFolder>

    suspend fun fetchFavorites(folderId: String): List<Content>

    override suspend fun fetchFavorites(): List<Content> {
        return fetchFavoriteFolders().flatMap { fetchFavorites(it.id) }.distinctBy { it.url }
    }
}

data class ContentFavoriteFolder(
    val id: String,
    val title: String,
    val count: Int? = null,
)
