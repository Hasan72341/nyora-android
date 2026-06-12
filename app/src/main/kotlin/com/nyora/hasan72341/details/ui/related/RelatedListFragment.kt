package com.nyora.hasan72341.details.ui.related

import android.view.Menu
import android.view.MenuInflater
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.ui.list.ListSelectionController
import com.nyora.hasan72341.list.ui.MangaListFragment

@AndroidEntryPoint
class RelatedListFragment : MangaListFragment() {

	override val viewModel by viewModels<RelatedListViewModel>()
	override val isSwipeRefreshEnabled = false

	override fun onScrolledToEnd() = Unit

	override fun onCreateActionMode(
		controller: ListSelectionController,
		menuInflater: MenuInflater,
		menu: Menu
	): Boolean {
		menuInflater.inflate(R.menu.mode_remote, menu)
		return super.onCreateActionMode(controller, menuInflater, menu)
	}
}

