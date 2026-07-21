package com.nyora.hasan72341.core.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.time.Instant

@Parcelize
data class MangaHistory(
	val createdAt: Instant,
	val updatedAt: Instant,
	val chapterId: String,
	val page: Int,
	val scroll: Int,
	val percent: Float,
	val chaptersCount: Int,
) : Parcelable
