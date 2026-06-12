package com.nyora.hasan72341.settings.sources.adapter

import com.nyora.hasan72341.core.ui.ReorderableListAdapter
import com.nyora.hasan72341.settings.sources.model.SourceConfigItem

class SourceConfigAdapter(
	listener: SourceConfigListener,
) : ReorderableListAdapter<SourceConfigItem>() {

	init {
		with(delegatesManager) {
			addDelegate(sourceConfigItemDelegate2(listener))
			addDelegate(sourceConfigEmptySearchDelegate())
			addDelegate(sourceConfigTipDelegate(listener))
		}
	}
}
