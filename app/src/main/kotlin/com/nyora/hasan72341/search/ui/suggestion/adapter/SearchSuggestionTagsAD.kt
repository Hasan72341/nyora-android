package com.nyora.hasan72341.search.ui.suggestion.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.nyora.hasan72341.core.ui.widgets.ChipsView
import com.nyora.hasan72341.databinding.ItemSearchSuggestionTagsBinding
import com.nyora.hasan72341.mihon.parsers.model.MangaTag
import com.nyora.hasan72341.search.ui.suggestion.SearchSuggestionListener
import com.nyora.hasan72341.search.ui.suggestion.model.SearchSuggestionItem

fun searchSuggestionTagsAD(
	listener: SearchSuggestionListener,
) = adapterDelegateViewBinding<SearchSuggestionItem.Tags, SearchSuggestionItem, ItemSearchSuggestionTagsBinding>(
	{ layoutInflater, parent -> ItemSearchSuggestionTagsBinding.inflate(layoutInflater, parent, false) },
) {

	binding.chipsGenres.onChipClickListener = ChipsView.OnChipClickListener { _, data ->
		listener.onTagClick(data as? MangaTag ?: return@OnChipClickListener)
	}

	bind {
		binding.chipsGenres.setChips(item.tags)
	}
}
