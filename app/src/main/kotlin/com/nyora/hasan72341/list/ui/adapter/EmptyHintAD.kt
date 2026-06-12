package com.nyora.hasan72341.list.ui.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.nyora.hasan72341.core.util.ext.setTextAndVisible
import com.nyora.hasan72341.databinding.ItemEmptyCardBinding
import com.nyora.hasan72341.list.ui.model.EmptyHint
import com.nyora.hasan72341.list.ui.model.ListModel

fun emptyHintAD(
	listener: ListStateHolderListener,
) = adapterDelegateViewBinding<EmptyHint, ListModel, ItemEmptyCardBinding>(
	{ inflater, parent -> ItemEmptyCardBinding.inflate(inflater, parent, false) },
) {

	binding.buttonRetry.setOnClickListener { listener.onEmptyActionClick() }

	bind {
		binding.icon.setImageAsync(item.icon)
		binding.textPrimary.setText(item.textPrimary)
		binding.textSecondary.setTextAndVisible(item.textSecondary)
		binding.buttonRetry.setTextAndVisible(item.actionStringRes)
	}
}
