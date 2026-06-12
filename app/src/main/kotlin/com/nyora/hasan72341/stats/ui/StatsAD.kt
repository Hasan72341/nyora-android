package com.nyora.hasan72341.stats.ui

import android.content.res.ColorStateList
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.ui.list.OnListItemClickListener
import com.nyora.hasan72341.core.util.NyoraColors
import com.nyora.hasan72341.databinding.ItemStatsBinding
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.stats.domain.StatsRecord

fun statsAD(
	listener: OnListItemClickListener<Manga>,
) = adapterDelegateViewBinding<StatsRecord, StatsRecord, ItemStatsBinding>(
	{ layoutInflater, parent -> ItemStatsBinding.inflate(layoutInflater, parent, false) },
) {

	binding.root.setOnClickListener { v ->
		listener.onItemClick(item.manga ?: return@setOnClickListener, v)
	}

	bind {
		binding.textViewTitle.text = item.manga?.title ?: getString(R.string.other_manga)
		binding.textViewSummary.text = item.time.format(context.resources)
		binding.imageViewBadge.imageTintList = ColorStateList.valueOf(NyoraColors.ofManga(context, item.manga))
		binding.root.isClickable = item.manga != null
	}
}
