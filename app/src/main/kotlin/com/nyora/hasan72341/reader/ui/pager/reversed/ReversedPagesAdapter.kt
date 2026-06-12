package com.nyora.hasan72341.reader.ui.pager.reversed

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import com.nyora.hasan72341.ai.MangaTranslator
import com.nyora.hasan72341.core.exceptions.resolve.ExceptionResolver
import com.nyora.hasan72341.core.os.NetworkState
import com.nyora.hasan72341.databinding.ItemPageBinding
import com.nyora.hasan72341.reader.domain.PageLoader
import com.nyora.hasan72341.reader.ui.config.ReaderSettings
import com.nyora.hasan72341.reader.ui.pager.BaseReaderAdapter

class ReversedPagesAdapter(
	private val lifecycleOwner: LifecycleOwner,
	loader: PageLoader,
	readerSettingsProducer: ReaderSettings.Producer,
	networkState: NetworkState,
	exceptionResolver: ExceptionResolver,
	private val translator: MangaTranslator,
) : BaseReaderAdapter<ReversedPageHolder>(loader, readerSettingsProducer, networkState, exceptionResolver) {

	override fun onCreateViewHolder(
		parent: ViewGroup,
		loader: PageLoader,
		readerSettingsProducer: ReaderSettings.Producer,
		networkState: NetworkState,
		exceptionResolver: ExceptionResolver,
	) = ReversedPageHolder(
		owner = lifecycleOwner,
		binding = ItemPageBinding.inflate(LayoutInflater.from(parent.context), parent, false),
		loader = loader,
		readerSettingsProducer = readerSettingsProducer,
		networkState = networkState,
		exceptionResolver = exceptionResolver,
		translator = translator,
	)
}
