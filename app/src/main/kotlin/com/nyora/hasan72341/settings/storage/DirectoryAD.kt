package com.nyora.hasan72341.settings.storage

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.nyora.hasan72341.core.ui.list.OnListItemClickListener
import com.nyora.hasan72341.core.util.ext.textAndVisible
import com.nyora.hasan72341.databinding.ItemStorageBinding

fun directoryAD(
	clickListener: OnListItemClickListener<DirectoryModel>,
) = adapterDelegateViewBinding<DirectoryModel, DirectoryModel, ItemStorageBinding>(
	{ layoutInflater, parent -> ItemStorageBinding.inflate(layoutInflater, parent, false) },
) {

	binding.root.setOnClickListener { v -> clickListener.onItemClick(item, v) }

	bind {
		binding.textViewTitle.text = item.title ?: getString(item.titleRes)
		binding.textViewSubtitle.textAndVisible = item.file?.absolutePath
		binding.imageViewIndicator.isChecked = item.isChecked
	}
}
