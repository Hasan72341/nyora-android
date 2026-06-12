package com.nyora.hasan72341.list.ui

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.nav.router
import com.nyora.hasan72341.favourites.ui.list.FavouritesListFragment
import com.nyora.hasan72341.history.ui.HistoryListFragment
import com.nyora.hasan72341.list.ui.config.ListConfigSection
import com.nyora.hasan72341.suggestions.ui.SuggestionsFragment
import com.nyora.hasan72341.tracker.ui.updates.UpdatesFragment

class MangaListMenuProvider(
	private val fragment: Fragment,
) : MenuProvider {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_list, menu)
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
		R.id.action_list_mode -> {
			val section: ListConfigSection = when (fragment) {
				is HistoryListFragment -> ListConfigSection.History
				is SuggestionsFragment -> ListConfigSection.Suggestions
				is FavouritesListFragment -> ListConfigSection.Favorites(fragment.categoryId)
				is UpdatesFragment -> ListConfigSection.Updated
				else -> ListConfigSection.General
			}
			fragment.router.showListConfigSheet(section)
			true
		}

		else -> false
	}
}
