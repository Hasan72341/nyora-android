package com.nyora.hasan72341.explore.ui.adapter

import android.view.View
import com.nyora.hasan72341.list.ui.adapter.ListHeaderClickListener
import com.nyora.hasan72341.list.ui.adapter.ListStateHolderListener

interface ExploreListEventListener : ListStateHolderListener, View.OnClickListener, ListHeaderClickListener
