package com.nyora.hasan72341.settings.sources

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import com.nyora.hasan72341.core.util.ext.consume
import kotlinx.coroutines.launch
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.parser.datadriven.DataDrivenCatalogueRepository
import com.nyora.hasan72341.core.prefs.AppSettings
import com.nyora.hasan72341.core.ui.sheet.BaseAdaptiveSheet
import com.nyora.hasan72341.databinding.SheetAddRepositoryBinding
import com.nyora.hasan72341.explore.data.MangaSourcesRepository
import javax.inject.Inject

/** Lets the user paste a source-catalogue URL to load its sources. */
@AndroidEntryPoint
class AddSourceRepositorySheet : BaseAdaptiveSheet<SheetAddRepositoryBinding>() {

	@Inject
	lateinit var settings: AppSettings

	@Inject
	lateinit var catalogue: DataDrivenCatalogueRepository

	@Inject
	lateinit var sourcesRepository: MangaSourcesRepository

	override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
		SheetAddRepositoryBinding.inflate(inflater, container, false)

	override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
		val typeMask = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
		requireViewBinding().root.updatePadding(bottom = insets.getInsets(typeMask).bottom)
		return insets.consume(v, typeMask, bottom = true)
	}

	override fun onViewBindingCreated(binding: SheetAddRepositoryBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		binding.editUrl.setText(settings.sourceCatalogueUrl)
		binding.editUrl.doAfterTextChanged { binding.layoutUrl.error = null }
		binding.buttonPaste.setOnClickListener { pasteFromClipboard() }
		binding.buttonAdd.setOnClickListener { addRepository() }
	}

	private fun pasteFromClipboard() {
		val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
		val text = clipboard?.primaryClip?.takeIf { it.itemCount > 0 }
			?.getItemAt(0)?.coerceToText(requireContext())?.toString()?.trim().orEmpty()
		if (text.isNotEmpty()) {
			requireViewBinding().editUrl.setText(text)
			requireViewBinding().editUrl.setSelection(text.length)
		}
	}

	private fun addRepository() {
		val binding = requireViewBinding()
		val url = binding.editUrl.text?.toString()?.trim().orEmpty()
		if (url.isEmpty()) {
			binding.layoutUrl.error = getString(R.string.source_repository_invalid)
			return
		}
		setLoading(true)
		lifecycleScope.launch {
			settings.sourceCatalogueUrl = url
			val count = catalogue.refresh().getOrDefault(0)
			sourcesRepository.assimilateFromCatalogue()
			setLoading(false)
			if (count > 0) {
				Toast.makeText(
					requireContext(),
					getString(R.string.source_repository_loaded, count),
					Toast.LENGTH_LONG,
				).show()
				dismiss()
			} else {
				binding.layoutUrl.error = getString(R.string.source_repository_invalid)
			}
		}
	}

	private fun setLoading(loading: Boolean) {
		val binding = viewBinding ?: return
		binding.progress.isVisible = loading
		binding.buttonAdd.text = if (loading) "" else getString(R.string.add_sources)
		binding.buttonAdd.isEnabled = !loading
		binding.buttonPaste.isEnabled = !loading
		binding.editUrl.isEnabled = !loading
	}
}
