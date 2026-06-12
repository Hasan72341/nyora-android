package com.nyora.hasan72341.filter.ui.tags

import android.content.Context
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.nyora.hasan72341.core.ui.BaseListAdapter
import com.nyora.hasan72341.core.ui.list.OnListItemClickListener
import com.nyora.hasan72341.core.ui.list.fastscroll.FastScroller
import com.nyora.hasan72341.core.util.ext.setChecked
import com.nyora.hasan72341.databinding.ItemCheckableNewBinding
import com.nyora.hasan72341.filter.ui.model.TagCatalogItem
import com.nyora.hasan72341.list.ui.ListModelDiffCallback
import com.nyora.hasan72341.list.ui.adapter.ListItemType
import com.nyora.hasan72341.list.ui.adapter.errorFooterAD
import com.nyora.hasan72341.list.ui.adapter.errorStateListAD
import com.nyora.hasan72341.list.ui.adapter.loadingFooterAD
import com.nyora.hasan72341.list.ui.adapter.loadingStateAD
import com.nyora.hasan72341.list.ui.model.ListModel

class TagsCatalogAdapter(
	listener: OnListItemClickListener<TagCatalogItem>,
) : BaseListAdapter<ListModel>(), FastScroller.SectionIndexer {

	init {
		addDelegate(ListItemType.FILTER_TAG, tagCatalogDelegate(listener))
		addDelegate(ListItemType.STATE_LOADING, loadingStateAD())
		addDelegate(ListItemType.FOOTER_LOADING, loadingFooterAD())
		addDelegate(ListItemType.FOOTER_ERROR, errorFooterAD(null))
		addDelegate(ListItemType.STATE_ERROR, errorStateListAD(null))
	}

	override fun getSectionText(context: Context, position: Int): CharSequence? {
		return (items.getOrNull(position) as? TagCatalogItem)?.tag?.title?.firstOrNull()?.uppercase()
	}

	private fun tagCatalogDelegate(
		listener: OnListItemClickListener<TagCatalogItem>,
	) = adapterDelegateViewBinding<TagCatalogItem, ListModel, ItemCheckableNewBinding>(
		{ layoutInflater, parent -> ItemCheckableNewBinding.inflate(layoutInflater, parent, false) },
	) {

		itemView.setOnClickListener {
			listener.onItemClick(item, itemView)
		}

		bind { payloads ->
			binding.root.text = item.tag.title
			binding.root.setChecked(item.isChecked, ListModelDiffCallback.PAYLOAD_CHECKED_CHANGED in payloads)
		}
	}
}
