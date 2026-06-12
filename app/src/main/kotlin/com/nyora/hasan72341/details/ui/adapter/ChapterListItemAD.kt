package com.nyora.hasan72341.details.ui.adapter

import android.graphics.Typeface
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.ui.list.AdapterDelegateClickListenerAdapter
import com.nyora.hasan72341.core.ui.list.OnListItemClickListener
import com.nyora.hasan72341.core.util.ext.drawableStart
import com.nyora.hasan72341.core.util.ext.getThemeColor
import com.nyora.hasan72341.core.util.ext.getThemeColorStateList
import com.nyora.hasan72341.core.util.ext.textAndVisible
import com.nyora.hasan72341.databinding.ItemChapterBinding
import com.nyora.hasan72341.details.ui.model.ChapterListItem
import com.nyora.hasan72341.list.ui.model.ListModel
import androidx.appcompat.R as appcompatR
import com.google.android.material.R as materialR

fun chapterListItemAD(
	clickListener: OnListItemClickListener<ChapterListItem>,
) = adapterDelegateViewBinding<ChapterListItem, ListModel, ItemChapterBinding>(
	viewBinding = { inflater, parent -> ItemChapterBinding.inflate(inflater, parent, false) },
	on = { item, _, _ -> item is ChapterListItem && !item.isGrid },
) {

	AdapterDelegateClickListenerAdapter(this, clickListener).attach(itemView)

	bind {
		binding.textViewTitle.text = item.getTitle(context.resources)
		binding.textViewDescription.textAndVisible = item.description
		when {
			item.isCurrent -> {
				binding.cardRoot.setCardBackgroundColor(context.getThemeColorStateList(materialR.attr.colorPrimaryContainer))
				binding.cardRoot.strokeColor = context.getThemeColor(appcompatR.attr.colorPrimary)
				binding.cardRoot.strokeWidth = (2 * context.resources.displayMetrics.density).toInt()

				binding.textViewTitle.drawableStart = ContextCompat.getDrawable(context, R.drawable.ic_current_chapter)
				binding.textViewTitle.setTextColor(context.getThemeColorStateList(materialR.attr.colorOnPrimaryContainer))
				binding.textViewDescription.setTextColor(context.getThemeColorStateList(materialR.attr.colorOnPrimaryContainer))
				binding.textViewTitle.typeface = Typeface.DEFAULT_BOLD
				binding.textViewDescription.typeface = Typeface.DEFAULT_BOLD
			}

			item.isUnread -> {
				binding.cardRoot.setCardBackgroundColor(context.getThemeColorStateList(materialR.attr.colorSurfaceVariant))
				binding.cardRoot.strokeColor = context.getThemeColor(materialR.attr.colorOutline)
				binding.cardRoot.strokeWidth = (1 * context.resources.displayMetrics.density).toInt()

				binding.textViewTitle.drawableStart = if (item.isNew) {
					ContextCompat.getDrawable(context, R.drawable.ic_new)
				} else {
					null
				}
				binding.textViewTitle.setTextColor(context.getThemeColorStateList(materialR.attr.colorOnSurface))
				binding.textViewDescription.setTextColor(context.getThemeColorStateList(materialR.attr.colorOutline))
				binding.textViewTitle.typeface = Typeface.DEFAULT
				binding.textViewDescription.typeface = Typeface.DEFAULT
			}

			else -> {
				binding.cardRoot.setCardBackgroundColor(context.getThemeColorStateList(android.R.attr.colorBackground))
				binding.cardRoot.strokeColor = context.getThemeColor(materialR.attr.colorOutlineVariant)
				binding.cardRoot.strokeWidth = (1 * context.resources.displayMetrics.density).toInt()

				binding.textViewTitle.drawableStart = null
				binding.textViewTitle.setTextColor(context.getThemeColorStateList(android.R.attr.textColorHint))
				binding.textViewDescription.setTextColor(context.getThemeColorStateList(android.R.attr.textColorHint))
				binding.textViewTitle.typeface = Typeface.DEFAULT
				binding.textViewDescription.typeface = Typeface.DEFAULT
			}
		}
		binding.imageViewBookmarked.isVisible = item.isBookmarked
		binding.imageViewDownloaded.isVisible = item.isDownloaded
	}
}

