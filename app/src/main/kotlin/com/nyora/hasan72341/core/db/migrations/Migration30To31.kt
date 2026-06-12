package com.nyora.hasan72341.core.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration30To31 : Migration(30, 31) {

    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Recreate 'manga' table with TEXT manga_id
        db.execSQL("CREATE TABLE manga_new (manga_id TEXT NOT NULL, title TEXT NOT NULL, alt_title TEXT, url TEXT NOT NULL, public_url TEXT NOT NULL, rating REAL NOT NULL, nsfw INTEGER NOT NULL, content_rating TEXT, cover_url TEXT NOT NULL, large_cover_url TEXT, state TEXT, authors TEXT, source TEXT NOT NULL, description TEXT NOT NULL DEFAULT '', tags TEXT NOT NULL DEFAULT '[]', chapters TEXT NOT NULL DEFAULT '[]', unread INTEGER NOT NULL DEFAULT 0, progress REAL NOT NULL DEFAULT 0, PRIMARY KEY(manga_id))")
        db.execSQL("INSERT INTO manga_new (manga_id, title, alt_title, url, public_url, rating, nsfw, content_rating, cover_url, large_cover_url, state, authors, source, description, tags, chapters, unread, progress) SELECT CAST(manga_id AS TEXT), title, alt_title, url, public_url, rating, nsfw, content_rating, cover_url, large_cover_url, state, authors, source, description, tags, chapters, unread, progress FROM manga")
        db.execSQL("DROP TABLE manga")
        db.execSQL("ALTER TABLE manga_new RENAME TO manga")

        // 2. Recreate 'history' table
        db.execSQL("CREATE TABLE history_new (manga_id TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, chapter_id TEXT NOT NULL, page INTEGER NOT NULL, scroll REAL NOT NULL, percent REAL NOT NULL, deleted_at INTEGER NOT NULL, chapters INTEGER NOT NULL, PRIMARY KEY(manga_id), FOREIGN KEY(manga_id) REFERENCES manga(manga_id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("INSERT INTO history_new (manga_id, created_at, updated_at, chapter_id, page, scroll, percent, deleted_at, chapters) SELECT CAST(manga_id AS TEXT), created_at, updated_at, CAST(chapter_id AS TEXT), page, scroll, percent, deleted_at, chapters FROM history")
        db.execSQL("DROP TABLE history")
        db.execSQL("ALTER TABLE history_new RENAME TO history")

        // 3. Recreate 'favourites' table
        db.execSQL("CREATE TABLE favourites_new (manga_id TEXT NOT NULL, category_id INTEGER NOT NULL, sort_key INTEGER NOT NULL, pinned INTEGER NOT NULL, created_at INTEGER NOT NULL, deleted_at INTEGER NOT NULL, PRIMARY KEY(manga_id, category_id), FOREIGN KEY(manga_id) REFERENCES manga(manga_id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(category_id) REFERENCES favourite_categories(category_id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("INSERT INTO favourites_new (manga_id, category_id, sort_key, pinned, created_at, deleted_at) SELECT CAST(manga_id AS TEXT), category_id, sort_key, pinned, created_at, deleted_at FROM favourites")
        db.execSQL("DROP TABLE favourites")
        db.execSQL("ALTER TABLE favourites_new RENAME TO favourites")
        db.execSQL("CREATE INDEX index_favourites_manga_id ON favourites(manga_id)")
        db.execSQL("CREATE INDEX index_favourites_category_id ON favourites(category_id)")

        // 4. Recreate 'bookmarks' table
        db.execSQL("CREATE TABLE bookmarks_new (page_id TEXT NOT NULL, manga_id TEXT NOT NULL, chapter_id TEXT NOT NULL, page INTEGER NOT NULL, scroll INTEGER NOT NULL, image TEXT NOT NULL, created_at INTEGER NOT NULL, percent REAL NOT NULL, deleted_at INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(page_id), FOREIGN KEY(manga_id) REFERENCES manga(manga_id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("INSERT INTO bookmarks_new (page_id, manga_id, chapter_id, page, scroll, image, created_at, percent, deleted_at) SELECT CAST(page_id AS TEXT), CAST(manga_id AS TEXT), CAST(chapter_id AS TEXT), page, scroll, image, created_at, percent, deleted_at FROM bookmarks")
        db.execSQL("DROP TABLE bookmarks")
        db.execSQL("ALTER TABLE bookmarks_new RENAME TO bookmarks")
        db.execSQL("CREATE INDEX index_bookmarks_page_id ON bookmarks(page_id)")
        db.execSQL("CREATE INDEX index_bookmarks_manga_id ON bookmarks(manga_id)")

        // 5. Recreate 'tracks' table
        db.execSQL("CREATE TABLE tracks_new (manga_id TEXT NOT NULL, last_chapter_id TEXT NOT NULL, chapters_new INTEGER NOT NULL, last_check_time INTEGER NOT NULL, last_chapter_date INTEGER NOT NULL, last_result INTEGER NOT NULL, last_error TEXT, PRIMARY KEY(manga_id), FOREIGN KEY(manga_id) REFERENCES manga(manga_id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("INSERT INTO tracks_new (manga_id, last_chapter_id, chapters_new, last_check_time, last_chapter_date, last_result, last_error) SELECT CAST(manga_id AS TEXT), CAST(last_chapter_id AS TEXT), chapters_new, last_check_time, last_chapter_date, last_result, last_error FROM tracks")
        db.execSQL("DROP TABLE tracks")
        db.execSQL("ALTER TABLE tracks_new RENAME TO tracks")

        // 6. Recreate 'scrobblings' table
        db.execSQL("CREATE TABLE scrobblings_new (scrobbler INTEGER NOT NULL, id INTEGER NOT NULL, manga_id TEXT NOT NULL, target_id INTEGER NOT NULL, status TEXT, chapter INTEGER NOT NULL, comment TEXT, rating REAL NOT NULL, PRIMARY KEY(scrobbler, id, manga_id))")
        db.execSQL("INSERT INTO scrobblings_new (scrobbler, id, manga_id, target_id, status, chapter, comment, rating) SELECT scrobbler, id, CAST(manga_id AS TEXT), target_id, status, chapter, comment, rating FROM scrobblings")
        db.execSQL("DROP TABLE scrobblings")
        db.execSQL("ALTER TABLE scrobblings_new RENAME TO scrobblings")

        // 7. Recreate 'preferences' table (TABLE_PREFERENCES is 'preferences')
        db.execSQL("CREATE TABLE preferences_new (manga_id TEXT NOT NULL, mode INTEGER NOT NULL, cf_brightness REAL NOT NULL, cf_contrast REAL NOT NULL, cf_invert INTEGER NOT NULL, cf_grayscale INTEGER NOT NULL, cf_book INTEGER NOT NULL, cf_multitone INTEGER NOT NULL DEFAULT 0, title_override TEXT, cover_override TEXT, content_rating_override TEXT, PRIMARY KEY(manga_id), FOREIGN KEY(manga_id) REFERENCES manga(manga_id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("INSERT INTO preferences_new (manga_id, mode, cf_brightness, cf_contrast, cf_invert, cf_grayscale, cf_book, cf_multitone, title_override, cover_override, content_rating_override) SELECT CAST(manga_id AS TEXT), mode, cf_brightness, cf_contrast, cf_invert, cf_grayscale, cf_book, cf_multitone, title_override, cover_override, content_rating_override FROM preferences")
        db.execSQL("DROP TABLE preferences")
        db.execSQL("ALTER TABLE preferences_new RENAME TO preferences")

        // 8. Recreate 'local_index' table
        db.execSQL("CREATE TABLE local_index_new (manga_id TEXT NOT NULL, path TEXT NOT NULL, PRIMARY KEY(manga_id), FOREIGN KEY(manga_id) REFERENCES manga(manga_id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("INSERT INTO local_index_new (manga_id, path) SELECT CAST(manga_id AS TEXT), path FROM local_index")
        db.execSQL("DROP TABLE local_index")
        db.execSQL("ALTER TABLE local_index_new RENAME TO local_index")
    }
}
