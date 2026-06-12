package com.nyora.hasan72341.mihon.model

import com.nyora.hasan72341.mihon.parsers.model.Content
import com.nyora.hasan72341.mihon.parsers.model.ContentChapter
import com.nyora.hasan72341.mihon.parsers.model.ContentListFilter
import com.nyora.hasan72341.mihon.parsers.model.ContentListFilterOptions
import com.nyora.hasan72341.mihon.parsers.model.ContentPage
import com.nyora.hasan72341.mihon.parsers.model.ContentRating
import com.nyora.hasan72341.mihon.parsers.model.ContentSource
import com.nyora.hasan72341.mihon.parsers.model.ContentState
import com.nyora.hasan72341.mihon.parsers.model.ContentTag
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.mihon.parsers.model.MangaChapter
import com.nyora.hasan72341.mihon.parsers.model.MangaListFilter
import com.nyora.hasan72341.mihon.parsers.model.MangaListFilterOptions
import com.nyora.hasan72341.mihon.parsers.model.MangaPage
import com.nyora.hasan72341.mihon.parsers.model.MangaSource
import com.nyora.hasan72341.mihon.parsers.model.MangaState
import com.nyora.hasan72341.mihon.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.model.MangaState as LibMangaState
import org.koitharu.kotatsu.parsers.model.MangaTag as LibMangaTag


fun Content.toManga(): Manga {
    return Manga(
        id = id,
        title = title,
        altTitles = altTitles.toList(),
        url = url,
        publicUrl = Url,
        rating = rating,
        contentRating = when (contentRating) {
            ContentRating.SAFE -> com.nyora.hasan72341.mihon.parsers.model.ContentRating.SAFE
            ContentRating.SUGGESTIVE -> com.nyora.hasan72341.mihon.parsers.model.ContentRating.SUGGESTIVE
            ContentRating.ADULT -> com.nyora.hasan72341.mihon.parsers.model.ContentRating.ADULT
            null -> null
        },
        coverUrl = coverUrl.orEmpty(),
        tags = tags.map { it.toMangaTag() },
        state = state?.toMangaState() ?: com.nyora.hasan72341.mihon.parsers.model.MangaState.ONGOING,
        authors = authors.toList(),
        largeCoverUrl = largeCoverUrl,
        description = description.orEmpty(),
        chapters = chapters?.map { it.toMangaChapter() } ?: emptyList()
    )
}

fun Manga.toContent(source: ContentSource): Content {
    return Content(
        id = id,
        title = title,
        altTitles = altTitles.toSet(),
        url = url,
        Url = publicUrl,
        rating = rating,
        contentRating = when (contentRating) {
            com.nyora.hasan72341.mihon.parsers.model.ContentRating.SAFE -> ContentRating.SAFE
            com.nyora.hasan72341.mihon.parsers.model.ContentRating.SUGGESTIVE -> ContentRating.SUGGESTIVE
            com.nyora.hasan72341.mihon.parsers.model.ContentRating.ADULT -> ContentRating.ADULT
            null -> null
        },
        coverUrl = coverUrl,
        tags = tags.map { it.toContentTag() }.toSet(),
        state = state?.toContentState(),
        authors = authors.toSet(),
        largeCoverUrl = largeCoverUrl,
        description = description,
        chapters = chapters.map { it.toContentChapter(source) },
        source = source
    )
}

fun ContentChapter.toMangaChapter(): MangaChapter {
    return MangaChapter(
        id = id.toString(),
        title = title ?: "",
        number = number,
        volume = volume,
        url = url,
        scanlator = scanlator,
        uploadDate = uploadDate,
        branch = branch
    )
}

fun MangaChapter.toContentChapter(source: ContentSource): ContentChapter {
    return ContentChapter(
        id = id,
        title = title,
        number = number,
        volume = volume,
        url = url,
        scanlator = scanlator,
        uploadDate = uploadDate,
        branch = branch,
        source = source
    )
}

fun ContentPage.toMangaPage(): MangaPage {
    return MangaPage(
        url = url,
        headers = emptyMap()
    )
}

fun MangaPage.toContentPage(source: ContentSource): ContentPage {
    return ContentPage(
        id = "",
        url = url,
        preview = "",
        headers = emptyMap(),
        source = source
    )
}

@OptIn(org.koitharu.kotatsu.parsers.InternalParsersApi::class)
fun ContentListFilterOptions.toMangaListFilterOptions(): MangaListFilterOptions {
    return MangaListFilterOptions(
        availableTags = availableTags.map { it.toLibMangaTag() }.toSet(),
        availableStates = availableStates.map { it.toLibMangaState() }.toSet()
    )
}

fun MangaListFilter.toContentListFilter(): ContentListFilter {
    return ContentListFilter(
        query = query,
        tags = tags.map { it.toContentTag() }.toSet(),
        tagsExclude = tagsExclude.map { it.toContentTag() }.toSet()
    )
}

private val DummyContentSource: ContentSource = object : ContentSource {
    override val name = "Dummy"
    override val locale = ""
    override val contentType = com.nyora.hasan72341.mihon.parsers.model.ContentType.OTHER
}

private fun ContentTag.toLibMangaTag(): LibMangaTag =
    LibMangaTag(title = title, key = key, source = com.nyora.hasan72341.core.model.UnknownMangaSource)

private fun ContentState.toLibMangaState(): LibMangaState = when (this) {
    ContentState.ONGOING -> LibMangaState.ONGOING
    ContentState.FINISHED -> LibMangaState.FINISHED
    ContentState.ABANDONED -> LibMangaState.ABANDONED
    ContentState.PAUSED -> LibMangaState.PAUSED
    ContentState.UPCOMING -> LibMangaState.UPCOMING
    ContentState.RESTRICTED -> LibMangaState.RESTRICTED
}

private fun LibMangaTag.toContentTag(): ContentTag =
    ContentTag(title = title, key = key, source = DummyContentSource)

fun ContentState.toMangaState(): MangaState {
    return when (this) {
        ContentState.ONGOING -> MangaState.ONGOING
        ContentState.FINISHED -> MangaState.FINISHED
        ContentState.ABANDONED -> MangaState.ABANDONED
        ContentState.PAUSED -> MangaState.PAUSED
        ContentState.UPCOMING -> MangaState.UPCOMING
        ContentState.RESTRICTED -> MangaState.RESTRICTED
    }
}

fun MangaState.toContentState(): ContentState {
    return when (this) {
        MangaState.ONGOING -> ContentState.ONGOING
        MangaState.FINISHED -> ContentState.FINISHED
        MangaState.ABANDONED -> ContentState.ABANDONED
        MangaState.PAUSED -> ContentState.PAUSED
        MangaState.UPCOMING -> ContentState.UPCOMING
        MangaState.RESTRICTED -> ContentState.RESTRICTED
    }
}

fun ContentTag.toMangaTag(): MangaTag {
    return MangaTag(
        title = title,
        key = key
    )
}

fun MangaTag.toContentTag(): ContentTag {
    return ContentTag(
        title = title,
        key = key,
        source = object : ContentSource {
            override val name = "Dummy"
            override val locale = ""
            override val contentType = com.nyora.hasan72341.mihon.parsers.model.ContentType.OTHER
        }
    )
}

fun ContentSource.toMangaSource(): MangaSource {
    return com.nyora.hasan72341.core.model.MangaSource(name)
}
