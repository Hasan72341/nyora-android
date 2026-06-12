package com.nyora.hasan72341.core.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * One-time cleanup of duplicate favourite categories that share a title.
 *
 * Every synced variant historically seeded its own default "Read later" category, so
 * Supabase accumulated several rows with the same title (one per device). Keep the
 * lowest id per title as the canonical category, move any favourites onto it, and
 * soft-delete the rest. The soft-delete now propagates via sync (see
 * SupabaseSync.pushCategories), so all devices converge on a single category.
 */
class Migration31To32 : Migration(31, 32) {

	override fun migrate(db: SupportSQLiteDatabase) {
		val now = System.currentTimeMillis()
		// Move favourites off the non-canonical duplicates onto the lowest-id category that
		// shares their title. OR IGNORE drops the move when the manga already sits in the canonical.
		db.execSQL(
			"UPDATE OR IGNORE favourites SET category_id = (" +
				"SELECT MIN(c.category_id) FROM favourite_categories c " +
				"WHERE c.deleted_at = 0 AND c.title = (" +
				"SELECT t.title FROM favourite_categories t WHERE t.category_id = favourites.category_id)) " +
				"WHERE category_id IN (" +
				"SELECT c.category_id FROM favourite_categories c WHERE c.deleted_at = 0 AND c.category_id <> (" +
				"SELECT MIN(c2.category_id) FROM favourite_categories c2 WHERE c2.deleted_at = 0 AND c2.title = c.title))"
		)
		// Soft-delete every category that is not the lowest id for its title.
		db.execSQL(
			"UPDATE favourite_categories SET deleted_at = $now WHERE deleted_at = 0 AND category_id <> (" +
				"SELECT MIN(c2.category_id) FROM favourite_categories c2 " +
				"WHERE c2.deleted_at = 0 AND c2.title = favourite_categories.title)"
		)
	}
}
