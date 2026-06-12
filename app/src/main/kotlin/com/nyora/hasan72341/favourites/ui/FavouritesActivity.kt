package com.nyora.hasan72341.favourites.ui

import android.os.Bundle
import com.nyora.hasan72341.core.nav.AppRouter
import com.nyora.hasan72341.core.ui.FragmentContainerActivity
import com.nyora.hasan72341.favourites.ui.list.FavouritesListFragment

class FavouritesActivity : FragmentContainerActivity(FavouritesListFragment::class.java) {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val categoryTitle = intent.getStringExtra(AppRouter.KEY_TITLE)
		if (categoryTitle != null) {
			title = categoryTitle
		}
	}
}
