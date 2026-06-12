package com.nyora.hasan72341.widget.shelf.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.ui.list.OnListItemClickListener
import com.nyora.hasan72341.databinding.ItemCategoryCheckableSingleBinding
import com.nyora.hasan72341.widget.shelf.model.CategoryItem

fun categorySelectItemAD(
	clickListener: OnListItemClickListener<CategoryItem>
) = adapterDelegateViewBinding<CategoryItem, CategoryItem, ItemCategoryCheckableSingleBinding>(
	{ inflater, parent -> ItemCategoryCheckableSingleBinding.inflate(inflater, parent, false) },
) {

	itemView.setOnClickListener {
		clickListener.onItemClick(item, it)
	}

	bind {
		with(binding.checkedTextView) {
			text = item.name ?: getString(R.string.all_favourites)
			isChecked = item.isSelected
		}
	}
}
