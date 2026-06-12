package com.nyora.hasan72341.bookmarks.domain

import android.database.SQLException
import androidx.room.withTransaction
import dagger.Reusable
import com.nyora.hasan72341.bookmarks.data.BookmarkEntity
import com.nyora.hasan72341.bookmarks.data.toBookmark
import com.nyora.hasan72341.bookmarks.data.toBookmarks
import com.nyora.hasan72341.bookmarks.data.toEntity
import com.nyora.hasan72341.core.db.MangaDatabase
import com.nyora.hasan72341.core.db.entity.toEntity
import com.nyora.hasan72341.core.db.entity.toManga
import com.nyora.hasan72341.core.ui.util.ReversibleHandle
import com.nyora.hasan72341.core.util.ext.mapItems
import com.nyora.hasan72341.core.util.ext.printStackTraceDebug
import com.nyora.hasan72341.mihon.parsers.model.Manga
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@Reusable
class BookmarksRepository @Inject constructor(
	private val db: MangaDatabase,
) {

	fun observeBookmark(manga: Manga, chapterId: String, page: Int): Flow<Bookmark?> {
		return db.getBookmarksDao().observe(manga.id, chapterId, page).map { it?.toBookmark(manga) }
	}

	fun observeBookmarks(manga: Manga): Flow<List<Bookmark>> {
		return db.getBookmarksDao().observe(manga.id).mapItems { it.toBookmark(manga) }
	}

	fun observeBookmarks(): Flow<Map<Manga, List<Bookmark>>> {
		return db.getBookmarksDao().observe().map { map ->
			val res = LinkedHashMap<Manga, List<Bookmark>>(map.size)
			for ((k, v) in map) {
				val manga = k.toManga()
				res[manga] = v.toBookmarks(manga)
			}
			res
		}
	}

	suspend fun addBookmark(bookmark: Bookmark) {
		db.withTransaction {
			db.getMangaDao().upsert(bookmark.manga.toEntity())
			db.getBookmarksDao().insert(bookmark.toEntity())
		}
	}

	suspend fun updateBookmark(bookmark: Bookmark, imageUrl: String) {
		val entity = bookmark.toEntity().copy(
			imageUrl = imageUrl,
		)
		db.getBookmarksDao().upsert(listOf(entity))
	}

	suspend fun removeBookmark(mangaId: String, chapterId: String, page: Int) {
		check(db.getBookmarksDao().delete(mangaId, chapterId, page) != 0) {
			"Bookmark not found"
		}
	}

	suspend fun removeBookmark(bookmark: Bookmark) {
		removeBookmark(bookmark.manga.id, bookmark.chapterId, bookmark.page)
	}

	suspend fun removeBookmarks(ids: Set<String>): ReversibleHandle {
		val entities = ArrayList<BookmarkEntity>(ids.size)
		db.withTransaction {
			val dao = db.getBookmarksDao()
			for (pageId in ids) {
				val e = dao.find(pageId)
				if (e != null) {
					entities.add(e)
				}
				dao.delete(pageId)
			}
		}
		return BookmarksRestorer(entities)
	}

	private inner class BookmarksRestorer(
		private val entities: Collection<BookmarkEntity>,
	) : ReversibleHandle {

		override suspend fun reverse() {
			db.withTransaction {
				for (e in entities) {
					try {
						db.getBookmarksDao().insert(e)
					} catch (e: SQLException) {
						e.printStackTraceDebug("BookmarksRepository::reverse")
					}
				}
			}
		}
	}
}
