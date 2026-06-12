package com.nyora.hasan72341.core.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration29To30 : Migration(29, 30) {

	override fun migrate(db: SupportSQLiteDatabase) {
		// Add soft-delete column to bookmarks for Supabase sync parity
		db.execSQL("ALTER TABLE bookmarks ADD COLUMN deleted_at INTEGER NOT NULL DEFAULT 0")
	}
}
