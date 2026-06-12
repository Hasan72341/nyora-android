package com.nyora.hasan72341.settings.tracker.categories

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.nyora.hasan72341.core.model.FavouriteCategory
import com.nyora.hasan72341.core.ui.list.AdapterDelegateClickListenerAdapter
import com.nyora.hasan72341.core.ui.list.OnListItemClickListener
import com.nyora.hasan72341.databinding.ItemCategoryCheckableMultipleBinding

fun trackerCategoryAD(
	listener: OnListItemClickListener<FavouriteCategory>,
) = adapterDelegateViewBinding<FavouriteCategory, FavouriteCategory, ItemCategoryCheckableMultipleBinding>(
	{ layoutInflater, parent -> ItemCategoryCheckableMultipleBinding.inflate(layoutInflater, parent, false) },
) {
	val eventListener = AdapterDelegateClickListenerAdapter(this, listener)
	itemView.setOnClickListener(eventListener)

	bind {
		binding.root.text = item.title
		binding.root.isChecked = item.isTrackingEnabled
	}
}
