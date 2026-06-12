package com.nyora.hasan72341.list.ui.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.nyora.hasan72341.core.util.ext.getDisplayMessage
import com.nyora.hasan72341.databinding.ItemErrorFooterBinding
import com.nyora.hasan72341.list.ui.model.ErrorFooter
import com.nyora.hasan72341.list.ui.model.ListModel

fun errorFooterAD(
	listener: ListStateHolderListener?,
) = adapterDelegateViewBinding<ErrorFooter, ListModel, ItemErrorFooterBinding>(
	{ inflater, parent -> ItemErrorFooterBinding.inflate(inflater, parent, false) },
) {

	if (listener != null) {
		binding.root.setOnClickListener {
			listener.onRetryClick(item.exception)
		}
	}

	bind {
		binding.textViewTitle.text = item.exception.getDisplayMessage(context.resources)
	}
}
