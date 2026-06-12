package com.nyora.hasan72341.scrobbling.common.ui.selector.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.nyora.hasan72341.core.util.ext.getDisplayMessage
import com.nyora.hasan72341.core.util.ext.setTextAndVisible
import com.nyora.hasan72341.core.util.ext.textAndVisible
import com.nyora.hasan72341.databinding.ItemEmptyHintBinding
import com.nyora.hasan72341.list.ui.adapter.ListStateHolderListener
import com.nyora.hasan72341.list.ui.model.ListModel
import com.nyora.hasan72341.scrobbling.common.ui.selector.model.ScrobblerHint

fun scrobblerHintAD(
	listener: ListStateHolderListener,
) = adapterDelegateViewBinding<ScrobblerHint, ListModel, ItemEmptyHintBinding>(
	{ inflater, parent -> ItemEmptyHintBinding.inflate(inflater, parent, false) },
) {

	binding.buttonRetry.setOnClickListener {
		val e = item.error
		if (e != null) {
			listener.onRetryClick(e)
		} else {
			listener.onEmptyActionClick()
		}
	}

	bind {
		binding.icon.setImageResource(item.icon)
		binding.textPrimary.setText(item.textPrimary)
		if (item.error != null) {
			binding.textSecondary.textAndVisible = item.error?.getDisplayMessage(context.resources)
		} else {
			binding.textSecondary.setTextAndVisible(item.textSecondary)
		}
		binding.buttonRetry.setTextAndVisible(item.actionStringRes)
	}
}
