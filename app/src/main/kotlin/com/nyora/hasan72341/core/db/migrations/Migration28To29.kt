package com.nyora.hasan72341.core.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration28To29 : Migration(28, 29) {

	override fun migrate(db: SupportSQLiteDatabase) {
		db.execSQL("ALTER TABLE preferences ADD COLUMN cf_multitone INTEGER NOT NULL DEFAULT 0")
	}
}
