package com.nyora.hasan72341.backups.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.json.JSONArray
import com.nyora.hasan72341.core.db.entity.MangaEntity

@Serializable
class MangaBackup(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("alt_title") val altTitles: String? = "[]",
    @SerialName("url") val url: String,
    @SerialName("public_url") val publicUrl: String,
    @SerialName("rating") val rating: Float = -1f,
    @SerialName("nsfw") val isNsfw: Boolean = false,
    @SerialName("content_rating") val contentRating: String? = null,
    @SerialName("cover_url") val coverUrl: String,
    @SerialName("large_cover_url") val largeCoverUrl: String? = null,
    @SerialName("state") val state: String? = null,
    @SerialName("author") val authors: String? = "[]",
    @SerialName("source") val source: String,
    @SerialName("description") val description: String = "",
    @SerialName("tags") val tags: Set<TagBackup> = emptySet(),
    @SerialName("chapters") val chapters: String = "[]",
    @SerialName("unread") val unread: Int = 0,
    @SerialName("progress") val progress: Float = 0f,
) {

    constructor(entity: MangaEntity) : this(
        id = entity.id,
        title = entity.title,
        altTitles = entity.altTitles,
        url = entity.url,
        publicUrl = entity.publicUrl,
        rating = entity.rating,
        isNsfw = entity.isNsfw,
        contentRating = entity.contentRating,
        coverUrl = entity.coverUrl,
        largeCoverUrl = entity.largeCoverUrl,
        state = entity.state,
        authors = entity.authors,
        source = entity.source,
        description = entity.description,
        tags = decodeTags(entity.tags),
        chapters = entity.chapters,
        unread = entity.unread,
        progress = entity.progress,
    )

    fun toEntity() = MangaEntity(
        id = id,
        title = title,
        altTitles = altTitles,
        url = url,
        publicUrl = publicUrl,
        rating = rating,
        isNsfw = isNsfw,
        contentRating = contentRating,
        coverUrl = coverUrl,
        largeCoverUrl = largeCoverUrl,
        state = state,
        authors = authors,
        source = source,
        description = description,
        tags = JSONArray().apply {
            for (tag in tags) {
                put(
                    org.json.JSONObject()
                        .put("key", tag.key)
                        .put("title", tag.title),
                )
            }
        }.toString(),
        chapters = chapters,
        unread = unread,
        progress = progress,
    )
}

private fun decodeTags(raw: String): Set<TagBackup> = runCatching {
    val array = JSONArray(raw)
    buildSet(array.length()) {
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            add(TagBackup(title = obj.optString("title"), key = obj.optString("key")))
        }
    }
}.getOrDefault(emptySet())
