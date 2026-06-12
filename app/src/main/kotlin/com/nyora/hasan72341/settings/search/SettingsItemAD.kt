package com.nyora.hasan72341.settings.search

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.ui.list.AdapterDelegateClickListenerAdapter
import com.nyora.hasan72341.core.ui.list.OnListItemClickListener
import com.nyora.hasan72341.core.util.ext.textAndVisible
import com.nyora.hasan72341.databinding.ItemPreferenceBinding

fun settingsItemAD(
	listener: OnListItemClickListener<SettingsItem>,
) = adapterDelegateViewBinding<SettingsItem, SettingsItem, ItemPreferenceBinding>(
	{ layoutInflater, parent -> ItemPreferenceBinding.inflate(layoutInflater, parent, false) },
) {

	AdapterDelegateClickListenerAdapter(this, listener).attach()
	val breadcrumbsSeparator = getString(R.string.breadcrumbs_separator)

	bind {
		binding.textViewTitle.text = item.title
		binding.textViewSummary.textAndVisible = item.breadcrumbs.joinToString(breadcrumbsSeparator)
	}
}
