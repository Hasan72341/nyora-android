package com.nyora.hasan72341.backups.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.nyora.hasan72341.mihon.parsers.model.MangaTag

@Serializable
class TagBackup(
	@SerialName("title") val title: String,
	@SerialName("key") val key: String,
	@SerialName("source") val source: String = "",
) {

	constructor(tag: MangaTag) : this(
		title = tag.title,
		key = tag.key,
	)

	fun toTag() = MangaTag(key = key, title = title)
}
