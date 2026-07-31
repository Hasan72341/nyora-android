package com.nyora.hasan72341.suggestions.ui

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.nyora.hasan72341.core.ui.BaseViewModel
import com.nyora.hasan72341.suggestions.data.AnilistRepository
import com.nyora.hasan72341.suggestions.domain.MangaSuggestionV2
import com.nyora.hasan72341.suggestions.domain.SmartMatchUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SuggestionsViewModel @Inject constructor(
    private val anilistRepository: AnilistRepository,
    private val smartMatchUseCase: SmartMatchUseCase,
) : BaseViewModel() {

    private val _suggestions = MutableStateFlow<List<MangaSuggestionV2>>(emptyList())
    val suggestions: StateFlow<List<MangaSuggestionV2>> = _suggestions.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        launchLoadingJob(Dispatchers.IO) {
            val trending = anilistRepository.getTrendingManga(15)
            val popular = anilistRepository.getPopularManga(15)
            val combined = (trending + popular).distinctBy { it.id }
            
            val initialList = combined.map { MangaSuggestionV2(it, isLoadingMatches = true) }
            _suggestions.value = initialList

            // Background matching
            initialList.forEach { suggestion ->
                val matches = smartMatchUseCase.findMatches(suggestion.anilistMedia)
                updateMatches(suggestion.anilistMedia.id, matches)
            }
        }
    }

    private suspend fun updateMatches(anilistId: Int, matches: List<com.nyora.hasan72341.mihon.parsers.model.Manga>) = withContext(Dispatchers.Main) {
        _suggestions.value = _suggestions.value.map {
            if (it.anilistMedia.id == anilistId) {
                it.copy(matches = matches, isLoadingMatches = false)
            } else {
                it
            }
        }
    }
}
