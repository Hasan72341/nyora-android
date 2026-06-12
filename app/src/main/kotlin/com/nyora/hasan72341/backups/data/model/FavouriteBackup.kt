package com.nyora.hasan72341.backups.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.nyora.hasan72341.favourites.data.FavouriteEntity
import com.nyora.hasan72341.favourites.data.FavouriteManga

@Serializable
class FavouriteBackup(
	@SerialName("manga_id") val mangaId: String,
	@SerialName("category_id") val categoryId: Long,
	@SerialName("sort_key") val sortKey: Int = 0,
	@SerialName("pinned") val isPinned: Boolean = false,
	@SerialName("created_at") val createdAt: Long,
	@SerialName("manga") val manga: MangaBackup,
) {

	constructor(entity: FavouriteManga) : this(
		mangaId = entity.manga.id,
		categoryId = entity.favourite.categoryId,
		sortKey = entity.favourite.sortKey,
		isPinned = entity.favourite.isPinned,
		createdAt = entity.favourite.createdAt,
		manga = MangaBackup(entity.manga),
	)

	fun toEntity() = FavouriteEntity(
		mangaId = mangaId,
		categoryId = categoryId,
		sortKey = sortKey,
		isPinned = isPinned,
		createdAt = createdAt,
		deletedAt = 0L,
	)
}
