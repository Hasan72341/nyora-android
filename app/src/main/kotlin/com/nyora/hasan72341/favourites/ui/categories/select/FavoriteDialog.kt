package com.nyora.hasan72341.favourites.ui.categories.select

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.nav.router
import com.nyora.hasan72341.core.ui.AlertDialogFragment
import com.nyora.hasan72341.core.ui.list.OnListItemClickListener
import com.nyora.hasan72341.core.util.ext.getDisplayMessage
import com.nyora.hasan72341.core.util.ext.joinToStringWithLimit
import com.nyora.hasan72341.core.util.ext.observe
import com.nyora.hasan72341.core.util.ext.observeEvent
import com.nyora.hasan72341.databinding.DialogFavoriteBinding
import com.nyora.hasan72341.favourites.ui.categories.select.adapter.MangaCategoriesAdapter
import com.nyora.hasan72341.favourites.ui.categories.select.model.MangaCategoryItem

@AndroidEntryPoint
class FavoriteDialog : AlertDialogFragment<DialogFavoriteBinding>(),
	OnListItemClickListener<MangaCategoryItem>, DialogInterface.OnClickListener {

	private val viewModel by viewModels<FavoriteDialogViewModel>()

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = DialogFavoriteBinding.inflate(inflater, container, false)

	override fun onBuildDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
		return super.onBuildDialog(builder)
			.setPositiveButton(R.string.done, null)
			.setNeutralButton(R.string.manage, this)
	}

	override fun onViewBindingCreated(
		binding: DialogFavoriteBinding,
		savedInstanceState: Bundle?,
	) {
		super.onViewBindingCreated(binding, savedInstanceState)
		val adapter = MangaCategoriesAdapter(this)
		binding.recyclerViewCategories.adapter = adapter
		viewModel.content.observe(viewLifecycleOwner, adapter)
		viewModel.onError.observeEvent(viewLifecycleOwner, ::onError)
		bindHeader()
	}

	override fun onItemClick(item: MangaCategoryItem, view: View) {
		viewModel.setChecked(item.category.id, item.checkedState != MaterialCheckBox.STATE_CHECKED)
	}

	override fun onClick(dialog: DialogInterface?, which: Int) {
		router.openFavoriteCategories()
	}

	private fun onError(e: Throwable) {
		Toast.makeText(context ?: return, e.getDisplayMessage(resources), Toast.LENGTH_SHORT).show()
	}

	private fun bindHeader() {
		val manga = viewModel.manga
		val binding = viewBinding ?: return
		binding.textViewTitle.text = manga.joinToStringWithLimit(binding.root.context, 92) { it.title }
		binding.coversStack.setCoversAsync(manga)
	}
}
