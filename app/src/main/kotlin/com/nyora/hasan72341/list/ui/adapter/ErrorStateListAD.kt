package com.nyora.hasan72341.list.ui.adapter

import android.view.View
import androidx.core.view.isVisible
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.util.ext.getDisplayMessage
import com.nyora.hasan72341.core.util.ext.setTextAndVisible
import com.nyora.hasan72341.databinding.ItemErrorStateBinding
import com.nyora.hasan72341.list.ui.model.ErrorState
import com.nyora.hasan72341.list.ui.model.ListModel

fun errorStateListAD(
	listener: ListStateHolderListener?,
) = adapterDelegateViewBinding<ErrorState, ListModel, ItemErrorStateBinding>(
	{ inflater, parent -> ItemErrorStateBinding.inflate(inflater, parent, false) },
) {

	if (listener != null) {
		val onClickListener = View.OnClickListener { v ->
			when (v.id) {
				R.id.button_retry -> listener.onRetryClick(item.exception)
				R.id.button_secondary -> listener.onSecondaryErrorActionClick(item.exception)
			}
		}

		binding.buttonRetry.setOnClickListener(onClickListener)
		binding.buttonSecondary.setOnClickListener(onClickListener)
	}

	bind {
		with(binding.textViewError) {
			text = item.exception.getDisplayMessage(context.resources)
			setCompoundDrawablesWithIntrinsicBounds(0, item.icon, 0, 0)
		}
		with(binding.buttonRetry) {
			isVisible = item.canRetry && listener != null
			setText(item.buttonText)
		}
		binding.buttonSecondary.setTextAndVisible(item.secondaryButtonText)
	}
}
