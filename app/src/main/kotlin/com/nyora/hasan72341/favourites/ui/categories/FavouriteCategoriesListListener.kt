package com.nyora.hasan72341.favourites.ui.categories

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.nyora.hasan72341.core.model.FavouriteCategory
import com.nyora.hasan72341.core.ui.list.OnListItemClickListener

interface FavouriteCategoriesListListener : OnListItemClickListener<FavouriteCategory?> {

	fun onDragHandleTouch(holder: RecyclerView.ViewHolder): Boolean

	fun onEditClick(item: FavouriteCategory, view: View)

	fun onShowAllClick(isChecked: Boolean)
}
