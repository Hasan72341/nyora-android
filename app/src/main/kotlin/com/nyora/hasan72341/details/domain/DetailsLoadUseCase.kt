package com.nyora.hasan72341.details.domain

import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.core.text.getSpans
import androidx.core.text.parseAsHtml
import coil3.request.CachePolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runInterruptible
import com.nyora.hasan72341.core.model.isLocal
import com.nyora.hasan72341.core.model.toMangaSource
import com.nyora.hasan72341.core.nav.MangaIntent
import com.nyora.hasan72341.core.os.NetworkState
import com.nyora.hasan72341.core.parser.CachingMangaRepository
import com.nyora.hasan72341.core.parser.MangaDataRepository
import com.nyora.hasan72341.core.parser.MangaRepository
import com.nyora.hasan72341.core.ui.model.MangaOverride
import com.nyora.hasan72341.core.util.ext.sanitize
import com.nyora.hasan72341.details.data.MangaDetails
import com.nyora.hasan72341.explore.domain.RecoverMangaUseCase
import com.nyora.hasan72341.local.data.LocalMangaRepository
import com.nyora.hasan72341.local.domain.model.LocalManga
import com.nyora.hasan72341.mihon.parsers.exception.NotFoundException
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.mihon.parsers.util.nullIfEmpty
import com.nyora.hasan72341.mihon.parsers.util.recoverNotNull
import com.nyora.hasan72341.mihon.parsers.util.runCatchingCancellable
import javax.inject.Inject

class DetailsLoadUseCase @Inject constructor(
	private val mangaDataRepository: MangaDataRepository,
	private val localMangaRepository: LocalMangaRepository,
	private val mangaRepositoryFactory: MangaRepository.Factory,
	private val recoverUseCase: RecoverMangaUseCase,
	private val imageGetter: Html.ImageGetter,
	private val networkState: NetworkState,
) {

	operator fun invoke(intent: MangaIntent, force: Boolean): Flow<MangaDetails> = flow {
		val manga = requireNotNull(mangaDataRepository.resolveIntent(intent, withChapters = true)) {
			"Cannot resolve intent $intent"
		}
		val override = mangaDataRepository.getOverride(manga.id)
		emit(
			MangaDetails(
				manga = manga,
				localManga = null,
				override = override,
				description = manga.description.parseAsHtml(withImages = false),
				isLoaded = false,
			),
		)
		if (manga.isLocal) {
			loadLocal(manga, override, force)
		} else {
			loadRemote(manga, override, force)
		}
	}.distinctUntilChanged()
		.flowOn(Dispatchers.IO)

	/**
	 * Load local manga + try to load the linked remote one if network is not restricted
	 * Suppress any network errors
	 */
	private suspend fun FlowCollector<MangaDetails>.loadLocal(manga: Manga, override: MangaOverride?, force: Boolean) {
		val skipNetworkLoad = !force && networkState.isOfflineOrRestricted()
		val localDetails = localMangaRepository.getDetails(manga)
		emit(
			MangaDetails(
				manga = localDetails,
				localManga = null,
				override = override,
					description = localDetails.description.parseAsHtml(withImages = false),
				isLoaded = skipNetworkLoad,
			),
		)
		if (skipNetworkLoad) {
			return
		}
		val remoteManga = localMangaRepository.getRemoteManga(manga)
		if (remoteManga == null) {
			emit(
				MangaDetails(
					manga = localDetails,
					localManga = null,
					override = override,
					description = localDetails.description.parseAsHtml(withImages = true),
					isLoaded = true,
				),
			)
		} else {
			val remoteDetails = getDetails(remoteManga, force).getOrNull()
			emit(
				MangaDetails(
					manga = remoteDetails ?: remoteManga,
					localManga = LocalManga(localDetails),
					override = override,
					description = (remoteDetails ?: localDetails).description.parseAsHtml(withImages = true),
					isLoaded = true,
				),
			)
			if (remoteDetails != null) {
				mangaDataRepository.updateChapters(remoteDetails)
			}
		}
	}

	/**
	 * Load remote manga + saved one if available
	 * Throw network errors after loading local manga only
	 */
	private suspend fun FlowCollector<MangaDetails>.loadRemote(
		manga: Manga,
		override: MangaOverride?,
		force: Boolean
	) = coroutineScope {
		val remoteDeferred = async {
			getDetails(manga, force)
		}
		val localManga = localMangaRepository.findSavedManga(manga, withDetails = true)
		if (localManga != null) {
			emit(
				MangaDetails(
					manga = manga,
					localManga = localManga,
					override = override,
					description = localManga.manga.description.parseAsHtml(withImages = true),
					isLoaded = false,
				),
			)
		}
		val remoteDetails = remoteDeferred.await().getOrThrow()
		emit(
			MangaDetails(
				manga = remoteDetails,
				localManga = localManga,
				override = override,
				description = (remoteDetails.description
					.ifEmpty { localManga?.manga?.description.orEmpty() }).parseAsHtml(withImages = true),
				isLoaded = true,
			),
		)
		mangaDataRepository.updateChapters(remoteDetails)
	}

	private suspend fun getDetails(seed: Manga, force: Boolean) = runCatchingCancellable {
		val repository = mangaRepositoryFactory.create(seed.source.toMangaSource())
		if (repository is CachingMangaRepository) {
			repository.getDetails(seed, if (force) CachePolicy.WRITE_ONLY else CachePolicy.ENABLED)
		} else {
			repository.getDetails(seed)
		}
	}.recoverNotNull { e ->
		if (e is NotFoundException) {
			recoverUseCase(seed)
		} else {
			null
		}
	}

	private suspend fun String.parseAsHtml(withImages: Boolean): CharSequence? = if (withImages) {
		runInterruptible(Dispatchers.IO) {
			parseAsHtml(imageGetter = imageGetter)
		}.filterSpans()
	} else {
		runInterruptible(Dispatchers.IO) {
			parseAsHtml()
		}.filterSpans().sanitize()
	}.trim().nullIfEmpty()

	private fun Spanned.filterSpans(): Spanned {
		val spannable = SpannableString.valueOf(this)
		val spans = spannable.getSpans<ForegroundColorSpan>()
		for (span in spans) {
			spannable.removeSpan(span)
		}
		return spannable
	}
}
