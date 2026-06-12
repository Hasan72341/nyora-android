package com.nyora.hasan72341.core.db

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

class DatabasePrePopulateCallback : RoomDatabase.Callback() {

	override fun onCreate(db: SupportSQLiteDatabase) {
		// No default favourite category is seeded: every synced device used to create its own
		// "Read later", and those independent rows piled up as duplicates once Supabase merged
		// them. Categories are now created by the user (or arrive via sync) and stay unique.
		val now = System.currentTimeMillis()
		db.execSQL(
			"INSERT INTO external_extension_repos (type, baseUrl, name, shortName, website, signingKeyFingerprint, createdAt, updatedAt, lastSuccessAt) VALUES (?,?,?,?,?,?,?,?,?)",
			arrayOf(
				"MIHON",
				"https://raw.githubusercontent.com/keiyoushi/extensions/refs/heads/repo",
				"Keiyoushi",
				"Keiyoushi",
				"https://keiyoushi.github.io/extensions",
				"508c909405615d0234a41316b230230559f6b9a89c3f15c13b306b38c2306f50",
				now,
				now,
				now,
			)
		)
	}
}
