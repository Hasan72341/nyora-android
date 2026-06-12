package com.nyora.hasan72341.favourites.data

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.nyora.hasan72341.core.db.entity.MangaEntity

class FavouriteManga(
	@Embedded val favourite: FavouriteEntity,
	@Relation(
		parentColumn = "manga_id",
		entityColumn = "manga_id"
	)
	val manga: MangaEntity,
	@Relation(
		parentColumn = "category_id",
		entityColumn = "category_id"
	)
	val categories: List<FavouriteCategoryEntity>
)