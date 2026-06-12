package com.nyora.hasan72341.backups.domain

import android.net.Uri
import java.util.Date

data class BackupFile(
	val uri: Uri,
	val dateTime: Date,
) : Comparable<BackupFile> {

	override fun compareTo(other: BackupFile): Int = compareValues(dateTime, other.dateTime)
}
