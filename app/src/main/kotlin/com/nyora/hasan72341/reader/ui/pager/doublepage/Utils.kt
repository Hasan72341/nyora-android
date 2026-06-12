package com.nyora.hasan72341.reader.ui.pager.doublepage

import androidx.core.view.children
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nyora.hasan72341.reader.ui.pager.BasePageHolder

fun RecyclerView.visiblePageHolders(): Sequence<BasePageHolder<*>> {
	val lm = layoutManager as? LinearLayoutManager ?: return emptySequence()
	return (lm.findFirstVisibleItemPosition()..lm.findLastVisibleItemPosition()).asSequence()
		.mapNotNull { findViewHolderForAdapterPosition(it) as? BasePageHolder<*> }
}

fun RecyclerView.allPageHolders(): Sequence<BasePageHolder<*>> {
	return children.mapNotNull {
		findContainingViewHolder(it) as? BasePageHolder<*>
	}
}
