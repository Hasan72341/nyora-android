package com.nyora.hasan72341.discover.ui.adapter

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.nyora.hasan72341.core.ui.BaseListAdapter
import com.nyora.hasan72341.databinding.ItemDiscoverHeroBinding
import com.nyora.hasan72341.databinding.ItemDiscoverRailBinding
import com.nyora.hasan72341.databinding.ItemDiscoverRailCardBinding
import com.nyora.hasan72341.databinding.ItemDiscoverRailErrorBinding
import com.nyora.hasan72341.databinding.ItemDiscoverRailShimmerBinding
import com.nyora.hasan72341.discover.ui.model.DiscoverHeroItem
import com.nyora.hasan72341.discover.ui.model.DiscoverRail
import com.nyora.hasan72341.discover.ui.model.DiscoverRailCard
import com.nyora.hasan72341.discover.ui.model.DiscoverRailError
import com.nyora.hasan72341.discover.ui.model.DiscoverRailLoading
import com.nyora.hasan72341.list.ui.adapter.ListItemType
import com.nyora.hasan72341.list.ui.model.ListModel

fun discoverHeroAD(
	onClick: (DiscoverHeroItem) -> Unit,
): AdapterDelegate<List<ListModel>> =
	adapterDelegateViewBinding<DiscoverHeroItem, ListModel, ItemDiscoverHeroBinding>(
		{ layoutInflater, parent -> ItemDiscoverHeroBinding.inflate(layoutInflater, parent, false) },
	) {

		binding.buttonAction.setOnClickListener { onClick(item) }

		bind {
			binding.imageViewCover.setImageAsync(item.coverUrl, null)
			binding.textViewTitle.text = item.title
			binding.textViewGenres.text = item.genres
		}
	}

fun discoverRailAD(
	cardClick: (DiscoverRailCard) -> Unit,
): AdapterDelegate<List<ListModel>> =
	adapterDelegateViewBinding<DiscoverRail, ListModel, ItemDiscoverRailBinding>(
		{ layoutInflater, parent -> ItemDiscoverRailBinding.inflate(layoutInflater, parent, false) },
	) {

		val adapter = BaseListAdapter<DiscoverRailCard>()
			.addDelegate(ListItemType.DISCOVER_RAIL, discoverRailCardAD(cardClick))
		binding.recyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
		binding.recyclerView.adapter = adapter
		binding.recyclerView.isNestedScrollingEnabled = false

		bind {
			adapter.items = item.items
		}
	}

fun discoverRailCardAD(
	cardClick: (DiscoverRailCard) -> Unit,
): AdapterDelegate<List<DiscoverRailCard>> =
	adapterDelegateViewBinding<DiscoverRailCard, DiscoverRailCard, ItemDiscoverRailCardBinding>(
		{ layoutInflater, parent -> ItemDiscoverRailCardBinding.inflate(layoutInflater, parent, false) },
	) {

		binding.root.setOnClickListener { cardClick(item) }

		bind {
			binding.imageViewCover.setImageAsync(item.coverUrl, item.manga)
			binding.textViewTitle.text = item.title
		}
	}

fun discoverRailLoadingAD(): AdapterDelegate<List<ListModel>> =
	adapterDelegateViewBinding<DiscoverRailLoading, ListModel, ItemDiscoverRailShimmerBinding>(
		{ layoutInflater, parent -> ItemDiscoverRailShimmerBinding.inflate(layoutInflater, parent, false) },
	) {
		// static shimmer placeholder; nothing to bind
	}

fun discoverRailErrorAD(
	onRetry: () -> Unit,
): AdapterDelegate<List<ListModel>> =
	adapterDelegateViewBinding<DiscoverRailError, ListModel, ItemDiscoverRailErrorBinding>(
		{ layoutInflater, parent -> ItemDiscoverRailErrorBinding.inflate(layoutInflater, parent, false) },
	) {

		binding.buttonRetry.setOnClickListener { onRetry() }
	}
