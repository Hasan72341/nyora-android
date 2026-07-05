package com.nyora.hasan72341.explore.ui.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.model.getSummary
import com.nyora.hasan72341.core.model.getTitle
import com.nyora.hasan72341.core.ui.BaseListAdapter
import com.nyora.hasan72341.core.ui.list.AdapterDelegateClickListenerAdapter
import com.nyora.hasan72341.core.ui.list.OnListItemClickListener
import com.nyora.hasan72341.core.util.ext.drawableStart
import com.nyora.hasan72341.core.util.ext.recyclerView
import com.nyora.hasan72341.core.util.ext.setProgressIcon
import com.nyora.hasan72341.core.util.ext.setTooltipCompat
import com.nyora.hasan72341.core.util.ext.textAndVisible
import com.nyora.hasan72341.databinding.ItemExploreButtonsBinding
import com.nyora.hasan72341.databinding.ItemExploreDemoBinding
import com.nyora.hasan72341.databinding.ItemExploreSourceGridBinding
import com.nyora.hasan72341.databinding.ItemExploreSourceListBinding
import com.nyora.hasan72341.databinding.ItemRecommendationBinding
import com.nyora.hasan72341.databinding.ItemRecommendationMangaBinding
import com.nyora.hasan72341.explore.ui.model.DemoBookItem
import com.nyora.hasan72341.explore.ui.model.ExploreButtons
import com.nyora.hasan72341.explore.ui.model.MangaSourceItem
import com.nyora.hasan72341.explore.ui.model.RecommendationsItem
import com.nyora.hasan72341.list.ui.adapter.ListItemType
import com.nyora.hasan72341.list.ui.model.ListModel
import com.nyora.hasan72341.list.ui.model.MangaCompactListModel
import com.nyora.hasan72341.mihon.parsers.model.Manga

fun exploreButtonsAD(
	clickListener: View.OnClickListener,
) = adapterDelegateViewBinding<ExploreButtons, ListModel, ItemExploreButtonsBinding>(
	{ layoutInflater, parent -> ItemExploreButtonsBinding.inflate(layoutInflater, parent, false) },
) {

	binding.buttonBookmarks.setOnClickListener(clickListener)
	binding.buttonDownloads.setOnClickListener(clickListener)
	binding.buttonLocal.setOnClickListener(clickListener)
	binding.buttonRandom.setOnClickListener(clickListener)

	bind {
		if (item.isRandomLoading) {
			binding.buttonRandom.setProgressIcon()
		} else {
			binding.buttonRandom.setIconResource(R.drawable.ic_dice)
		}
		binding.buttonRandom.isClickable = !item.isRandomLoading
	}
}

fun exploreRecommendationItemAD(
	itemClickListener: OnListItemClickListener<Manga>,
) = adapterDelegateViewBinding<RecommendationsItem, ListModel, ItemRecommendationBinding>(
	{ layoutInflater, parent -> ItemRecommendationBinding.inflate(layoutInflater, parent, false) },
) {

	val adapter = BaseListAdapter<MangaCompactListModel>()
		.addDelegate(ListItemType.MANGA_LIST, recommendationMangaItemAD(itemClickListener))
	binding.pager.adapter = adapter
	binding.pager.recyclerView?.isNestedScrollingEnabled = false
	binding.dots.bindToViewPager(binding.pager)

	bind {
		adapter.items = item.manga
	}
}

fun recommendationMangaItemAD(
	itemClickListener: OnListItemClickListener<Manga>,
) = adapterDelegateViewBinding<MangaCompactListModel, MangaCompactListModel, ItemRecommendationMangaBinding>(
	{ layoutInflater, parent -> ItemRecommendationMangaBinding.inflate(layoutInflater, parent, false) },
) {

	binding.root.setOnClickListener { v ->
		itemClickListener.onItemClick(item.manga, v)
	}
	bind {
		binding.textViewTitle.text = item.manga.title
		binding.textViewSubtitle.textAndVisible = item.subtitle
		binding.imageViewCover.setImageAsync(item.manga.coverUrl, item.manga)
	}
}


fun exploreSourceListItemAD(
	listener: OnListItemClickListener<MangaSourceItem>,
) = adapterDelegateViewBinding<MangaSourceItem, ListModel, ItemExploreSourceListBinding>(
	{ layoutInflater, parent ->
		ItemExploreSourceListBinding.inflate(
			layoutInflater,
			parent,
			false,
		)
	},
	on = { item, _, _ -> item is MangaSourceItem && !item.isGrid },
) {

	AdapterDelegateClickListenerAdapter(this, listener).attach(itemView)
	val iconPinned = ContextCompat.getDrawable(context, R.drawable.ic_pin_small)

	bind {
		binding.textViewTitle.text = item.source.getTitle(context)
		binding.textViewTitle.drawableStart = if (item.source.isPinned) iconPinned else null
		binding.textViewSubtitle.text = item.source.getSummary(context)
		binding.imageViewIcon.setImageAsync(item.source)
	}
}

fun exploreSourceGridItemAD(
	listener: OnListItemClickListener<MangaSourceItem>,
) = adapterDelegateViewBinding<MangaSourceItem, ListModel, ItemExploreSourceGridBinding>(
	{ layoutInflater, parent ->
		ItemExploreSourceGridBinding.inflate(
			layoutInflater,
			parent,
			false,
		)
	},
	on = { item, _, _ -> item is MangaSourceItem && item.isGrid },
) {

	AdapterDelegateClickListenerAdapter(this, listener).attach(itemView)
	val iconPinned = ContextCompat.getDrawable(context, R.drawable.ic_pin_small)

	bind {
		val title = item.source.getTitle(context)
		val summary = item.source.getSummary(context)
		itemView.setTooltipCompat(
			buildSpannedString {
				bold {
					append(title)
				}
				if (summary != null) {
					appendLine()
					append(summary)
				}
			},
		)
		binding.textViewTitle.text = title
		binding.textViewTitle.drawableStart = if (item.source.isPinned) iconPinned else null
		binding.imageViewIcon.setImageAsync(item.source)
	}
}

fun exploreDemoItemAD(
	listener: OnListItemClickListener<DemoBookItem>,
) = adapterDelegateViewBinding<DemoBookItem, ListModel, ItemExploreDemoBinding>(
	{ layoutInflater, parent -> ItemExploreDemoBinding.inflate(layoutInflater, parent, false) },
) {

	AdapterDelegateClickListenerAdapter(this, listener).attach(itemView)

	bind {
		binding.textViewTitle.text = item.title
		binding.textViewSubtitle.text = item.subtitle
		binding.textViewGlyph.text = item.glyph
		val accent = item.accentColor
		val darker = Color.rgb(
			(Color.red(accent) * 0.6f).toInt(),
			(Color.green(accent) * 0.6f).toInt(),
			(Color.blue(accent) * 0.6f).toInt(),
		)
		binding.textViewGlyph.background = GradientDrawable(
			GradientDrawable.Orientation.TL_BR,
			intArrayOf(accent, darker),
		).apply {
			cornerRadius = context.resources.displayMetrics.density * 12f
		}
	}
}
