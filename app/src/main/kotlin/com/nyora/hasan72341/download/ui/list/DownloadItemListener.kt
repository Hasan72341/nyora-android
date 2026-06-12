package com.nyora.hasan72341.download.ui.list

import com.nyora.hasan72341.core.ui.list.OnListItemClickListener

interface DownloadItemListener : OnListItemClickListener<DownloadItemModel> {

	fun onCancelClick(item: DownloadItemModel)

	fun onPauseClick(item: DownloadItemModel)

	fun onResumeClick(item: DownloadItemModel)

	fun onSkipClick(item: DownloadItemModel)

	fun onSkipAllClick(item: DownloadItemModel)

	fun onExpandClick(item: DownloadItemModel)
}
