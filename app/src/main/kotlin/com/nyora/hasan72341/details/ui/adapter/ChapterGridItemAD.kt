package com.nyora.hasan72341.details.ui.adapter

import android.graphics.Typeface
import androidx.core.view.isVisible
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.nyora.hasan72341.core.ui.list.AdapterDelegateClickListenerAdapter
import com.nyora.hasan72341.core.ui.list.OnListItemClickListener
import com.nyora.hasan72341.core.util.ext.getThemeColor
import com.nyora.hasan72341.core.util.ext.getThemeColorStateList
import com.nyora.hasan72341.core.util.ext.setTooltipCompat
import com.nyora.hasan72341.core.model.numberString
import com.nyora.hasan72341.databinding.ItemChapterGridBinding
import com.nyora.hasan72341.details.ui.model.ChapterListItem
import com.nyora.hasan72341.list.ui.model.ListModel
import androidx.appcompat.R as appcompatR
import com.google.android.material.R as materialR

fun chapterGridItemAD(
	clickListener: OnListItemClickListener<ChapterListItem>,
) = adapterDelegateViewBinding<ChapterListItem, ListModel, ItemChapterGridBinding>(
	viewBinding = { inflater, parent -> ItemChapterGridBinding.inflate(inflater, parent, false) },
	on = { item, _, _ -> item is ChapterListItem && item.isGrid },
) {

	AdapterDelegateClickListenerAdapter(this, clickListener).attach(itemView)

	bind { payloads ->
		if (payloads.isEmpty()) {
			binding.textViewTitle.text = item.chapter.numberString() ?: "?"
			itemView.setTooltipCompat(item.chapter.title)
		}
		binding.imageViewNew.isVisible = item.isNew
		binding.imageViewCurrent.isVisible = item.isCurrent
		binding.imageViewBookmarked.isVisible = item.isBookmarked
		binding.imageViewDownloaded.isVisible = item.isDownloaded

		when {
			item.isCurrent -> {
				binding.root.setCardBackgroundColor(context.getThemeColorStateList(materialR.attr.colorPrimaryContainer))
				binding.root.strokeColor = context.getThemeColor(appcompatR.attr.colorPrimary)
				binding.root.strokeWidth = (2 * context.resources.displayMetrics.density).toInt()

				binding.textViewTitle.setTextColor(context.getThemeColorStateList(materialR.attr.colorOnPrimaryContainer))
				binding.textViewTitle.typeface = Typeface.DEFAULT_BOLD
			}

			item.isUnread -> {
				binding.root.setCardBackgroundColor(context.getThemeColorStateList(materialR.attr.colorSurfaceVariant))
				binding.root.strokeColor = context.getThemeColor(materialR.attr.colorOutline)
				binding.root.strokeWidth = (1 * context.resources.displayMetrics.density).toInt()

				binding.textViewTitle.setTextColor(context.getThemeColorStateList(materialR.attr.colorOnSurface))
				binding.textViewTitle.typeface = Typeface.DEFAULT
			}

			else -> {
				binding.root.setCardBackgroundColor(context.getThemeColorStateList(android.R.attr.colorBackground))
				binding.root.strokeColor = context.getThemeColor(materialR.attr.colorOutlineVariant)
				binding.root.strokeWidth = (1 * context.resources.displayMetrics.density).toInt()

				binding.textViewTitle.setTextColor(context.getThemeColorStateList(android.R.attr.textColorHint))
				binding.textViewTitle.typeface = Typeface.DEFAULT
			}
		}
	}
}

