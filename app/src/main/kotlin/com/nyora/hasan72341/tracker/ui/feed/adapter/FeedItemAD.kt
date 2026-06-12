package com.nyora.hasan72341.tracker.ui.feed.adapter

import androidx.core.content.ContextCompat
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.ui.list.OnListItemClickListener
import com.nyora.hasan72341.core.util.ext.drawableStart
import com.nyora.hasan72341.core.util.ext.getQuantityStringSafe
import com.nyora.hasan72341.databinding.ItemFeedBinding
import com.nyora.hasan72341.list.ui.model.ListModel
import com.nyora.hasan72341.tracker.ui.feed.model.FeedItem

fun feedItemAD(
	clickListener: OnListItemClickListener<FeedItem>,
) = adapterDelegateViewBinding<FeedItem, ListModel, ItemFeedBinding>(
	{ inflater, parent -> ItemFeedBinding.inflate(inflater, parent, false) },
) {
	val indicatorNew = ContextCompat.getDrawable(context, R.drawable.ic_new)

	itemView.setOnClickListener {
		clickListener.onItemClick(item, it)
	}

	bind {
		binding.imageViewCover.setImageAsync(item.imageUrl, item.manga)
		binding.textViewTitle.text = item.title
		binding.textViewSummary.text = context.resources.getQuantityStringSafe(
			R.plurals.new_chapters,
			item.count,
			item.count,
		)
		binding.textViewSummary.drawableStart = if (item.isNew) {
			indicatorNew
		} else {
			null
		}
	}
}
