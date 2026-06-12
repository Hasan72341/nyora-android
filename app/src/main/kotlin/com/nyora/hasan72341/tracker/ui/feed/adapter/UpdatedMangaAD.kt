package com.nyora.hasan72341.tracker.ui.feed.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.ui.BaseListAdapter
import com.nyora.hasan72341.core.ui.list.OnListItemClickListener
import com.nyora.hasan72341.databinding.ItemListGroupBinding
import com.nyora.hasan72341.list.ui.adapter.ListHeaderClickListener
import com.nyora.hasan72341.list.ui.adapter.ListItemType
import com.nyora.hasan72341.list.ui.adapter.mangaGridItemAD
import com.nyora.hasan72341.list.ui.model.ListHeader
import com.nyora.hasan72341.list.ui.model.ListModel
import com.nyora.hasan72341.list.ui.model.MangaListModel
import com.nyora.hasan72341.list.ui.size.ItemSizeResolver
import com.nyora.hasan72341.tracker.ui.feed.model.UpdatedMangaHeader

fun updatedMangaAD(
	sizeResolver: ItemSizeResolver,
	listener: OnListItemClickListener<MangaListModel>,
	headerClickListener: ListHeaderClickListener,
) = adapterDelegateViewBinding<UpdatedMangaHeader, ListModel, ItemListGroupBinding>(
	{ layoutInflater, parent -> ItemListGroupBinding.inflate(layoutInflater, parent, false) },
) {

	val adapter = BaseListAdapter<ListModel>()
		.addDelegate(ListItemType.MANGA_GRID, mangaGridItemAD(sizeResolver, listener))
	binding.recyclerView.adapter = adapter
	binding.buttonMore.setOnClickListener { v ->
		headerClickListener.onListHeaderClick(ListHeader(0, payload = item), v)
	}
	binding.textViewTitle.setText(R.string.updates)
	binding.buttonMore.setText(R.string.more)

	bind {
		adapter.items = item.list
	}
}
