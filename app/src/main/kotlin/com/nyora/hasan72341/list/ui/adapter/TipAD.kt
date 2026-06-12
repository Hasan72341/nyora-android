package com.nyora.hasan72341.list.ui.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.nyora.hasan72341.core.ui.widgets.TipView
import com.nyora.hasan72341.databinding.ItemTip2Binding
import com.nyora.hasan72341.list.ui.model.ListModel
import com.nyora.hasan72341.list.ui.model.TipModel

fun tipAD(
	listener: TipView.OnButtonClickListener,
) = adapterDelegateViewBinding<TipModel, ListModel, ItemTip2Binding>(
	{ layoutInflater, parent -> ItemTip2Binding.inflate(layoutInflater, parent, false) }
) {

	binding.root.onButtonClickListener = listener

	bind {
		with(binding.root) {
			tag = item
			setTitle(item.title)
			setText(item.text)
			setIcon(item.icon)
			setPrimaryButtonText(item.primaryButtonText)
			setSecondaryButtonText(item.secondaryButtonText)
		}
	}
}
