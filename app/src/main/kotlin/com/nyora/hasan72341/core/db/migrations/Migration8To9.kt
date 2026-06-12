package com.nyora.hasan72341.core.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nyora.hasan72341.mihon.parsers.model.SortOrder

class Migration8To9 : Migration(8, 9) {

	override fun migrate(db: SupportSQLiteDatabase) {
		db.execSQL("ALTER TABLE favourite_categories ADD COLUMN `order` TEXT NOT NULL DEFAULT ${SortOrder.NEWEST.name}")
	}
}
