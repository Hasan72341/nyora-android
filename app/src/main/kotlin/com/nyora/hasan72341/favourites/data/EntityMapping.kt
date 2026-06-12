package com.nyora.hasan72341.favourites.data

import com.nyora.hasan72341.core.db.entity.toManga
import com.nyora.hasan72341.core.model.FavouriteCategory
import com.nyora.hasan72341.list.domain.ListSortOrder
import java.time.Instant

fun FavouriteCategoryEntity.toFavouriteCategory(id: Long = categoryId.toLong()) = FavouriteCategory(
	id = id,
	title = title,
	sortKey = sortKey,
	order = ListSortOrder(order, ListSortOrder.NEWEST),
	createdAt = Instant.ofEpochMilli(createdAt),
	isTrackingEnabled = track,
	isVisibleInLibrary = isVisibleInLibrary,
)

fun FavouriteManga.toManga() = manga.toManga()

fun Collection<FavouriteManga>.toMangaList() = map { it.toManga() }
