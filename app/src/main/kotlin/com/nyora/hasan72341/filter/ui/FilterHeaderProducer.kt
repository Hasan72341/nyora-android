package com.nyora.hasan72341.filter.ui

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.model.titleResId
import com.nyora.hasan72341.core.ui.widgets.ChipsView
import com.nyora.hasan72341.filter.data.PersistableFilter
import com.nyora.hasan72341.filter.ui.model.FilterHeaderModel
import com.nyora.hasan72341.filter.ui.model.FilterProperty
import com.nyora.hasan72341.mihon.parsers.model.MangaListFilter
import com.nyora.hasan72341.mihon.parsers.model.MangaListFilterCapabilities
import com.nyora.hasan72341.mihon.parsers.model.MangaSource
import com.nyora.hasan72341.mihon.parsers.model.MangaTag
import com.nyora.hasan72341.mihon.parsers.model.ContentType
import com.nyora.hasan72341.mihon.parsers.model.ContentRating
import com.nyora.hasan72341.mihon.parsers.model.Demographic
import com.nyora.hasan72341.mihon.parsers.model.MangaState
import com.nyora.hasan72341.mihon.parsers.util.toTitleCase
import com.nyora.hasan72341.search.domain.MangaSearchRepository
import javax.inject.Inject
import androidx.appcompat.R as appcompatR

class FilterHeaderProducer @Inject constructor(
    private val searchRepository: MangaSearchRepository,
) {

    fun observeHeader(filterCoordinator: FilterCoordinator): Flow<FilterHeaderModel> {
        return combine(
            filterCoordinator.savedFilters,
            filterCoordinator.tags,
            filterCoordinator.observe(),
        ) { saved, tags, snapshot ->
            val chipList = createChipsList(
                source = filterCoordinator.mangaSource,
                capabilities = filterCoordinator.capabilities,
                savedFilters = saved,
                tagsProperty = tags,
                snapshot = snapshot.listFilter,
                limit = 12,
            )
            FilterHeaderModel(
                chips = chipList,
                sortOrder = snapshot.sortOrder,
                isFilterApplied = !snapshot.listFilter.isEmpty(),
            )
        }
    }

    private suspend fun createChipsList(
        source: MangaSource,
        capabilities: MangaListFilterCapabilities,
        savedFilters: FilterProperty<PersistableFilter>,
        tagsProperty: FilterProperty<MangaTag>,
        snapshot: MangaListFilter,
        limit: Int,
    ): List<ChipsView.ChipModel> {
        val result = ArrayDeque<ChipsView.ChipModel>(savedFilters.availableItems.size + limit + 3)
        if (snapshot.query.isNullOrEmpty() || capabilities.isSearchWithFiltersSupported) {
            val selectedTags = tagsProperty.selectedItems.toMutableSet()
            var tags = if (selectedTags.isEmpty()) {
                searchRepository.getTagsSuggestion("", limit, source)
            } else {
                searchRepository.getTagsSuggestion(selectedTags).take(limit)
            }
            if (tags.size < limit) {
                tags = tags + tagsProperty.availableItems.take(limit - tags.size)
            }
            if (tags.isEmpty() && selectedTags.isEmpty()) {
                return emptyList()
            }
            for (saved in savedFilters.availableItems) {
                val model = ChipsView.ChipModel(
                    title = saved.name,
                    isChecked = saved in savedFilters.selectedItems,
                    data = saved,
                )
                if (model.isChecked) {
                    selectedTags.removeAll(saved.filter.tags.map { MangaTag(key = it.key, title = it.title) }.toSet())
                    result.addFirst(model)
                } else {
                    result.addLast(model)
                }
            }
            for (tag in tags) {
                val model = ChipsView.ChipModel(
                    title = tag.title,
                    isChecked = selectedTags.remove(tag),
                    data = tag,
                )
                if (model.isChecked) {
                    result.addFirst(model)
                } else {
                    result.addLast(model)
                }
            }
            for (tag in selectedTags) {
                val model = ChipsView.ChipModel(
                    title = tag.title,
                    isChecked = true,
                    data = tag,
                )
                result.addFirst(model)
            }
        }
        snapshot.locale?.let {
            result.addFirst(
                ChipsView.ChipModel(
                    title = it.getDisplayName(it).toTitleCase(it),
                    icon = R.drawable.ic_language,
                    isCloseable = true,
                    data = it,
                ),
            )
        }
        snapshot.types.forEach {
            result.addFirst(
                ChipsView.ChipModel(
                    titleResId = it.toNyora().titleResId,
                    isCloseable = true,
                    data = it,
                ),
            )
        }
        snapshot.demographics.forEach {
            result.addFirst(
                ChipsView.ChipModel(
                    titleResId = it.toNyora().titleResId,
                    isCloseable = true,
                    data = it,
                ),
            )
        }
        snapshot.contentRating.forEach {
            result.addFirst(
                ChipsView.ChipModel(
                    titleResId = it.toNyora().titleResId,
                    isCloseable = true,
                    data = it,
                ),
            )
        }
        snapshot.states.forEach {
            result.addFirst(
                ChipsView.ChipModel(
                    titleResId = it.toNyora().titleResId,
                    isCloseable = true,
                    data = it,
                ),
            )
        }
        if (!snapshot.query.isNullOrEmpty()) {
            result.addFirst(
                ChipsView.ChipModel(
                    title = snapshot.query,
                    icon = appcompatR.drawable.abc_ic_search_api_material,
                    isCloseable = true,
                    data = snapshot.query,
                ),
            )
        }
        if (!snapshot.author.isNullOrEmpty()) {
            result.addFirst(
                ChipsView.ChipModel(
                    title = snapshot.author,
                    icon = R.drawable.ic_user,
                    isCloseable = true,
                    data = snapshot.author,
                ),
            )
        }
        val hasTags = result.any { it.data is MangaTag }
        if (hasTags) {
            result.addFirst(moreTagsChip())
        }
        return result
    }

    private fun moreTagsChip() = ChipsView.ChipModel(
        titleResId = R.string.genres,
        icon = R.drawable.ic_drawer_menu_open,
    )

    private fun org.koitharu.kotatsu.parsers.model.ContentType.toNyora(): ContentType = when (this) {
        org.koitharu.kotatsu.parsers.model.ContentType.MANGA -> ContentType.MANGA
        org.koitharu.kotatsu.parsers.model.ContentType.MANHWA -> ContentType.MANHWA
        org.koitharu.kotatsu.parsers.model.ContentType.MANHUA -> ContentType.MANHUA
        org.koitharu.kotatsu.parsers.model.ContentType.HENTAI -> ContentType.HENTAI_MANGA
        org.koitharu.kotatsu.parsers.model.ContentType.COMICS -> ContentType.COMICS
        org.koitharu.kotatsu.parsers.model.ContentType.NOVEL -> ContentType.NOVEL
        org.koitharu.kotatsu.parsers.model.ContentType.ONE_SHOT -> ContentType.ONE_SHOT
        org.koitharu.kotatsu.parsers.model.ContentType.DOUJINSHI -> ContentType.DOUJINSHI
        org.koitharu.kotatsu.parsers.model.ContentType.IMAGE_SET -> ContentType.IMAGE_SET
        org.koitharu.kotatsu.parsers.model.ContentType.ARTIST_CG -> ContentType.ARTIST_CG
        org.koitharu.kotatsu.parsers.model.ContentType.GAME_CG -> ContentType.GAME_CG
        org.koitharu.kotatsu.parsers.model.ContentType.OTHER -> ContentType.OTHER
    }

    private fun org.koitharu.kotatsu.parsers.model.ContentRating.toNyora(): ContentRating =
        ContentRating.valueOf(name)

    private fun org.koitharu.kotatsu.parsers.model.Demographic.toNyora(): Demographic =
        Demographic.valueOf(name)

    private fun org.koitharu.kotatsu.parsers.model.MangaState.toNyora(): MangaState =
        MangaState.valueOf(name)
}
