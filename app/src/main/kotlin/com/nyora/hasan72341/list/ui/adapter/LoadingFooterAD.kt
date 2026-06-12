package com.nyora.hasan72341.list.ui.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegate
import com.nyora.hasan72341.R
import com.nyora.hasan72341.list.ui.model.ListModel
import com.nyora.hasan72341.list.ui.model.LoadingFooter

fun loadingFooterAD() = adapterDelegate<LoadingFooter, ListModel>(R.layout.item_loading_footer) {
}