package com.nyora.hasan72341.picker.ui.manga

import android.view.View
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import com.nyora.hasan72341.R
import com.nyora.hasan72341.list.ui.MangaListFragment
import com.nyora.hasan72341.list.ui.model.MangaListModel
import com.nyora.hasan72341.picker.ui.PageImagePickActivity

@AndroidEntryPoint
class MangaPickerFragment : MangaListFragment() {

	override val isSwipeRefreshEnabled = false

	override val viewModel by viewModels<MangaPickerViewModel>()

	override fun onScrolledToEnd() = Unit

	override fun onItemClick(item: MangaListModel, view: View) {
		(activity as PageImagePickActivity).onMangaPicked(item.manga)
	}

	override fun onResume() {
		super.onResume()
		activity?.setTitle(R.string.pick_manga_page)
	}

	override fun onItemLongClick(item: MangaListModel, view: View): Boolean = false

	override fun onItemContextClick(item: MangaListModel, view: View): Boolean = false
}
