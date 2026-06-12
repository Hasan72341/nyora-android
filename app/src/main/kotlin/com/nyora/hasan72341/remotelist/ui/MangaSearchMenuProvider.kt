package com.nyora.hasan72341.remotelist.ui

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import androidx.core.view.inputmethod.EditorInfoCompat
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.ui.util.ReversibleAction
import com.nyora.hasan72341.core.util.ext.call
import com.nyora.hasan72341.filter.ui.FilterCoordinator
import com.nyora.hasan72341.list.ui.MangaListViewModel
import com.nyora.hasan72341.mihon.parsers.model.MangaListFilter

class MangaSearchMenuProvider(
	private val filter: FilterCoordinator,
	private val viewModel: MangaListViewModel,
) : MenuProvider, MenuItem.OnActionExpandListener, SearchView.OnQueryTextListener {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_search, menu)
		val menuItem = menu.findItem(R.id.action_search)
		menuItem.setOnActionExpandListener(this)
		val searchView = menuItem.actionView as SearchView
		searchView.setOnQueryTextListener(this)
		searchView.queryHint = menuItem.title
	}

	override fun onPrepareMenu(menu: Menu) {
		super.onPrepareMenu(menu)
		menu.findItem(R.id.action_search)?.isVisible = filter.capabilities.isSearchSupported
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false

	override fun onQueryTextSubmit(query: String?): Boolean {
		val snapshot = filter.snapshot()
		if (!query.isNullOrEmpty() && !filter.capabilities.isSearchWithFiltersSupported && snapshot.listFilter.hasNonSearchOptions()) {
			filter.set(MangaListFilter(query = query))
			viewModel.onActionDone.call(
				ReversibleAction(R.string.filter_search_warning) { filter.set(snapshot.listFilter) },
			)
		} else {
			filter.setQuery(query)
		}
		return true
	}

	override fun onQueryTextChange(newText: String?): Boolean = false

	override fun onMenuItemActionExpand(item: MenuItem): Boolean {
		(item.actionView as? SearchView)?.run {
			post { adjustSearchView() }
		}
		return true
	}

	override fun onMenuItemActionCollapse(item: MenuItem): Boolean = true

	private fun SearchView.adjustSearchView() {
		imeOptions = if (viewModel.isIncognitoModeEnabled) {
			imeOptions or EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING
		} else {
			imeOptions and EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING.inv()
		}
		setQuery(filter.query.value, false)
	}
}
