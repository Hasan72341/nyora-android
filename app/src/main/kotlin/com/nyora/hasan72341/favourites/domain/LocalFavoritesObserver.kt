package com.nyora.hasan72341.favourites.domain

import dagger.Reusable
import kotlinx.coroutines.flow.Flow
import com.nyora.hasan72341.core.db.MangaDatabase
import com.nyora.hasan72341.core.db.entity.toManga
import com.nyora.hasan72341.favourites.data.FavouriteManga
import com.nyora.hasan72341.list.domain.ListFilterOption
import com.nyora.hasan72341.list.domain.ListSortOrder
import com.nyora.hasan72341.local.data.index.LocalMangaIndex
import com.nyora.hasan72341.local.domain.LocalObserveMapper
import com.nyora.hasan72341.mihon.parsers.model.Manga
import javax.inject.Inject

@Reusable
class LocalFavoritesObserver @Inject constructor(
	localMangaIndex: LocalMangaIndex,
	private val db: MangaDatabase,
) : LocalObserveMapper<FavouriteManga, Manga>(localMangaIndex) {

	fun observeAll(
		order: ListSortOrder,
		filterOptions: Set<ListFilterOption>,
		limit: Int
	): Flow<List<Manga>> = db.getFavouritesDao().observeAll(order, filterOptions, limit).mapToLocal()

	fun observeAll(
		categoryId: Long,
		order: ListSortOrder,
		filterOptions: Set<ListFilterOption>,
		limit: Int
	): Flow<List<Manga>> = db.getFavouritesDao().observeAll(categoryId, order, filterOptions, limit).mapToLocal()

	override fun toManga(e: FavouriteManga) = e.manga.toManga()

	override fun toResult(e: FavouriteManga, manga: Manga) = manga
}
