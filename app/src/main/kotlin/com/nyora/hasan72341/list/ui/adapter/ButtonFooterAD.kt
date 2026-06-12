package com.nyora.hasan72341.list.ui.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.nyora.hasan72341.databinding.ItemButtonFooterBinding
import com.nyora.hasan72341.list.ui.model.ButtonFooter
import com.nyora.hasan72341.list.ui.model.ListModel

fun buttonFooterAD(
	listener: ListStateHolderListener,
) = adapterDelegateViewBinding<ButtonFooter, ListModel, ItemButtonFooterBinding>(
	{ inflater, parent -> ItemButtonFooterBinding.inflate(inflater, parent, false) },
) {

	binding.button.setOnClickListener {
		listener.onFooterButtonClick()
	}

	bind {
		binding.button.setText(item.textResId)
	}
}
