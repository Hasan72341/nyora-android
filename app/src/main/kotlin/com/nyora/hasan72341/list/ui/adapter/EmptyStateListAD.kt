package com.nyora.hasan72341.list.ui.adapter

import androidx.core.view.isVisible
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.nyora.hasan72341.core.util.ext.setTextAndVisible
import com.nyora.hasan72341.databinding.ItemEmptyStateBinding
import com.nyora.hasan72341.list.ui.model.EmptyState
import com.nyora.hasan72341.list.ui.model.ListModel

fun emptyStateListAD(
	listener: ListStateHolderListener?,
) = adapterDelegateViewBinding<EmptyState, ListModel, ItemEmptyStateBinding>(
	{ inflater, parent -> ItemEmptyStateBinding.inflate(inflater, parent, false) },
) {

	if (listener != null) {
		binding.buttonRetry.setOnClickListener { listener.onEmptyActionClick() }
	}

	bind {
		if (item.icon == 0) {
			binding.icon.isVisible = false
			binding.icon.disposeImage()
		} else {
			binding.icon.isVisible = true
			binding.icon.setImageAsync(item.icon)
		}
		binding.textPrimary.setText(item.textPrimary)
		binding.textSecondary.setTextAndVisible(item.textSecondary)
		if (listener != null) {
			binding.buttonRetry.setTextAndVisible(item.actionStringRes)
		}
	}
}
