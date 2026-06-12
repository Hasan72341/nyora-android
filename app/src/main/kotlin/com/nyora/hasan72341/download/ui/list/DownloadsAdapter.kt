package com.nyora.hasan72341.download.ui.list

import androidx.lifecycle.LifecycleOwner
import com.nyora.hasan72341.core.ui.BaseListAdapter
import com.nyora.hasan72341.list.ui.adapter.ListItemType
import com.nyora.hasan72341.list.ui.adapter.emptyStateListAD
import com.nyora.hasan72341.list.ui.adapter.listHeaderAD
import com.nyora.hasan72341.list.ui.adapter.loadingStateAD
import com.nyora.hasan72341.list.ui.model.ListModel

class DownloadsAdapter(
	lifecycleOwner: LifecycleOwner,
	listener: DownloadItemListener,
) : BaseListAdapter<ListModel>() {

	init {
		addDelegate(ListItemType.DOWNLOAD, downloadItemAD(lifecycleOwner, listener))
		addDelegate(ListItemType.STATE_LOADING, loadingStateAD())
		addDelegate(ListItemType.STATE_EMPTY, emptyStateListAD(null))
		addDelegate(ListItemType.HEADER, listHeaderAD(null))
	}
}
