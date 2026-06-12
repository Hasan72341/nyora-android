package com.nyora.hasan72341.list.ui.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.nyora.hasan72341.core.util.ext.setTextAndVisible
import com.nyora.hasan72341.databinding.ItemInfoBinding
import com.nyora.hasan72341.list.ui.model.InfoModel
import com.nyora.hasan72341.list.ui.model.ListModel

fun infoAD() = adapterDelegateViewBinding<InfoModel, ListModel, ItemInfoBinding>(
	{ layoutInflater, parent -> ItemInfoBinding.inflate(layoutInflater, parent, false) },
) {

	bind {
		binding.textViewTitle.setText(item.title)
		binding.textViewBody.setTextAndVisible(item.text)
		binding.textViewTitle.setCompoundDrawablesRelativeWithIntrinsicBounds(
			item.icon, 0, 0, 0,
		)
	}
}
