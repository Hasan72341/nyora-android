package com.nyora.hasan72341.details.ui.pager.pages

import coil3.size.Size
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.ui.list.AdapterDelegateClickListenerAdapter
import com.nyora.hasan72341.core.ui.list.OnListItemClickListener
import com.nyora.hasan72341.core.util.ext.setTextColorAttr
import com.nyora.hasan72341.databinding.ItemPageThumbBinding
import com.nyora.hasan72341.list.ui.model.ListModel
import com.google.android.material.R as materialR

fun pageThumbnailAD(
	clickListener: OnListItemClickListener<PageThumbnail>,
) = adapterDelegateViewBinding<PageThumbnail, ListModel, ItemPageThumbBinding>(
	{ inflater, parent -> ItemPageThumbBinding.inflate(inflater, parent, false) },
) {

	val gridWidth = itemView.context.resources.getDimensionPixelSize(R.dimen.preferred_grid_width)
	binding.imageViewThumb.exactImageSize = Size(
		width = gridWidth,
		height = (gridWidth / 13f * 18f).toInt(),
	)

	AdapterDelegateClickListenerAdapter(this, clickListener).attach(itemView)

	bind {
		binding.imageViewThumb.setImageAsync(item.page)
		with(binding.textViewNumber) {
			setBackgroundResource(if (item.isCurrent) R.drawable.bg_badge_accent else R.drawable.bg_badge_empty)
			setTextColorAttr(if (item.isCurrent) materialR.attr.colorOnTertiary else android.R.attr.textColorPrimary)
			text = item.number.toString()
		}
	}
}
