package com.nyora.hasan72341.core.prefs

import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.Keep
import androidx.annotation.StringRes
import com.nyora.hasan72341.R

@Keep
enum class NavItem(
	@IdRes val id: Int,
	@StringRes val title: Int,
	@DrawableRes val icon: Int,
) {

	HISTORY(R.id.nav_history, R.string.history, R.drawable.ic_nav_history),
	FAVORITES(R.id.nav_favorites, R.string.favourites, R.drawable.ic_nav_favorites),
	LOCAL(R.id.nav_local, R.string.on_device, R.drawable.ic_nav_local),
	EXPLORE(R.id.nav_explore, R.string.explore, R.drawable.ic_nav_explore),
	DISCOVER(R.id.nav_feed, R.string.discover, R.drawable.ic_nav_discover),
	UPDATED(R.id.nav_updated, R.string.updated, R.drawable.ic_nav_updated),
	BOOKMARKS(R.id.nav_bookmarks, R.string.bookmarks, R.drawable.ic_nav_bookmark),
	;

	fun isAvailable(settings: AppSettings): Boolean = when (this) {
		UPDATED, DISCOVER -> settings.isTrackerEnabled
		else -> true
	}
}
