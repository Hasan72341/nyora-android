package com.nyora.hasan72341.mihon.model

import android.os.Parcelable
import com.nyora.hasan72341.list.domain.ListSortOrder
import com.nyora.hasan72341.list.ui.ListModelDiffCallback
import com.nyora.hasan72341.list.ui.model.ListModel
import kotlinx.parcelize.Parcelize
import java.time.Instant

@Parcelize
data class FavouriteCategory(
    val id: Long,
    val title: String,
    val sortKey: Int,
    val order: ListSortOrder,
    val createdAt: Instant,
    val isTrackingEnabled: Boolean,
    val isVisibleInLibrary: Boolean,
) : Parcelable, ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is FavouriteCategory && id == other.id
	}

	override fun getChangePayload(previousState: ListModel): Any? {
		if (previousState !is FavouriteCategory) {
			return null
		}
		return if (isTrackingEnabled != previousState.isTrackingEnabled || isVisibleInLibrary != previousState.isVisibleInLibrary) {
			ListModelDiffCallback.PAYLOAD_CHECKED_CHANGED
		} else {
			null
		}
	}
}
