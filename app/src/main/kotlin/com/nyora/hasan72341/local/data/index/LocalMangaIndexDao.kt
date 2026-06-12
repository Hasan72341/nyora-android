package com.nyora.hasan72341.local.data.index

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface LocalMangaIndexDao {

	@Query("SELECT path FROM local_index WHERE manga_id = :mangaId")
	suspend fun findPath(mangaId: String): String?

	@Query("SELECT '' WHERE 0")
	suspend fun findTags(): List<String>

	@Query("SELECT '' WHERE 0 AND :isNsfw = 0")
	suspend fun findTags(isNsfw: Boolean): List<String>

	@Upsert
	suspend fun upsert(entity: LocalMangaIndexEntity)

	@Query("DELETE FROM local_index WHERE manga_id = :mangaId")
	suspend fun delete(mangaId: String)

	@Query("DELETE FROM local_index")
	suspend fun clear()
}
