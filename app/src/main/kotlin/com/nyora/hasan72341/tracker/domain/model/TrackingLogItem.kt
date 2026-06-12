package com.nyora.hasan72341.tracker.domain.model

import com.nyora.hasan72341.mihon.parsers.model.Manga
import java.time.Instant

data class TrackingLogItem(
	val id: Long,
	val manga: Manga,
	val chapters: List<String>,
	val createdAt: Instant,
	val isNew: Boolean,
)
