package com.nyora.hasan72341.reader.ui

import com.nyora.hasan72341.reader.domain.toChapterKey
import android.net.Uri
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.plus
import com.nyora.hasan72341.R
import com.nyora.hasan72341.bookmarks.domain.Bookmark
import com.nyora.hasan72341.bookmarks.domain.BookmarksRepository
import com.nyora.hasan72341.core.exceptions.EmptyMangaException
import com.nyora.hasan72341.core.model.getPreferredBranch
import com.nyora.hasan72341.core.nav.MangaIntent
import com.nyora.hasan72341.core.nav.ReaderIntent
import com.nyora.hasan72341.core.os.AppShortcutManager
import com.nyora.hasan72341.core.parser.MangaDataRepository
import com.nyora.hasan72341.core.parser.MangaRepository
import com.nyora.hasan72341.core.prefs.AppSettings
import com.nyora.hasan72341.core.prefs.ReaderMode
import com.nyora.hasan72341.core.prefs.TriStateOption
import com.nyora.hasan72341.core.prefs.observeAsFlow
import com.nyora.hasan72341.core.prefs.observeAsStateFlow
import com.nyora.hasan72341.core.util.ext.MutableEventFlow
import com.nyora.hasan72341.core.util.ext.call
import com.nyora.hasan72341.core.util.ext.firstNotNull
import com.nyora.hasan72341.core.util.ext.requireValue
import com.nyora.hasan72341.details.data.MangaDetails
import com.nyora.hasan72341.details.domain.DetailsInteractor
import com.nyora.hasan72341.details.domain.DetailsLoadUseCase
import com.nyora.hasan72341.details.domain.ProgressUpdateUseCase
import com.nyora.hasan72341.details.ui.pager.ChaptersPagesViewModel
import com.nyora.hasan72341.details.ui.pager.EmptyMangaReason
import com.nyora.hasan72341.download.ui.worker.DownloadWorker
import com.nyora.hasan72341.history.data.HistoryRepository
import com.nyora.hasan72341.history.domain.HistoryUpdateUseCase
import com.nyora.hasan72341.list.domain.ReadingProgress.Companion.PROGRESS_NONE
import com.nyora.hasan72341.local.data.LocalStorageChanges
import com.nyora.hasan72341.local.domain.DeleteLocalMangaUseCase
import com.nyora.hasan72341.local.domain.model.LocalManga
import com.nyora.hasan72341.mihon.parsers.model.ContentRating
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.mihon.parsers.model.MangaPage
import com.nyora.hasan72341.mihon.parsers.util.ifNullOrEmpty
import com.nyora.hasan72341.mihon.parsers.util.runCatchingCancellable
import com.nyora.hasan72341.reader.domain.ChaptersLoader
import com.nyora.hasan72341.reader.domain.DetectReaderModeUseCase
import com.nyora.hasan72341.reader.domain.PageLoader
import com.nyora.hasan72341.reader.domain.readerChapters
import com.nyora.hasan72341.reader.domain.readerChaptersFor
import com.nyora.hasan72341.reader.ui.config.ReaderSettings
import com.nyora.hasan72341.reader.ui.pager.ReaderUiState
import com.nyora.hasan72341.scrobbling.discord.ui.DiscordRpc
import com.nyora.hasan72341.stats.domain.StatsCollector
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

private const val BOUNDS_PAGE_OFFSET = 2
private const val PREFETCH_LIMIT = 10

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val dataRepository: MangaDataRepository,
    private val historyRepository: HistoryRepository,
    private val bookmarksRepository: BookmarksRepository,
    settings: AppSettings,
    private val pageLoader: PageLoader,
    private val chaptersLoader: ChaptersLoader,
    private val appShortcutManager: AppShortcutManager,
    private val detailsLoadUseCase: DetailsLoadUseCase,
    private val historyUpdateUseCase: HistoryUpdateUseCase,
    private val detectReaderModeUseCase: DetectReaderModeUseCase,
    private val progressUpdateUseCase: ProgressUpdateUseCase,
    private val statsCollector: StatsCollector,
    private val discordRpc: DiscordRpc,
    @LocalStorageChanges localStorageChanges: SharedFlow<LocalManga?>,
    interactor: DetailsInteractor,
    deleteLocalMangaUseCase: DeleteLocalMangaUseCase,
    downloadScheduler: DownloadWorker.Scheduler,
    private val mangaRepositoryFactory: MangaRepository.Factory,
    private val translator: com.nyora.hasan72341.ai.MangaTranslator,
    readerSettingsProducerFactory: ReaderSettings.Producer.Factory,
) : ChaptersPagesViewModel(
    settings = settings,
    interactor = interactor,
    bookmarksRepository = bookmarksRepository,
    historyRepository = historyRepository,
    downloadScheduler = downloadScheduler,
    deleteLocalMangaUseCase = deleteLocalMangaUseCase,
    localStorageChanges = localStorageChanges,
) {
    private val intent = MangaIntent(savedStateHandle)

    private var loadingJob: Job? = null
    private var pageSaveJob: Job? = null
    private var bookmarkJob: Job? = null
    private var stateChangeJob: Job? = null
    private var pretranslationJob: Job? = null
    private val pretranslatingChapters = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    init {
        mangaDetails.value = intent.manga?.let { MangaDetails(it) }
    }

    val readerMode = MutableStateFlow<ReaderMode?>(null)
    val onPageSaved = MutableEventFlow<Collection<Uri>>()
    val onLoadingError = MutableEventFlow<Throwable>()
    val onShowToast = MutableEventFlow<Int>()
    val onAskNsfwIncognito = MutableEventFlow<Unit>()
    val uiState = MutableStateFlow<ReaderUiState?>(null)

    val isIncognitoMode = MutableStateFlow(savedStateHandle.get<Boolean>(ReaderIntent.EXTRA_INCOGNITO))

    val content = MutableStateFlow(ReaderContent(emptyList(), null))

    val pageAnimation = settings.observeAsStateFlow(
        scope = viewModelScope + Dispatchers.IO,
        key = AppSettings.KEY_READER_ANIMATION,
        valueProducer = { readerAnimation },
    )

    val isInfoBarEnabled = settings.observeAsStateFlow(
        scope = viewModelScope + Dispatchers.IO,
        key = AppSettings.KEY_READER_BAR,
        valueProducer = { isReaderBarEnabled },
    )

    val isInfoBarTransparent = settings.observeAsStateFlow(
        scope = viewModelScope + Dispatchers.IO,
        key = AppSettings.KEY_READER_BAR_TRANSPARENT,
        valueProducer = { isReaderBarTransparent },
    )

    val isKeepScreenOnEnabled = settings.observeAsStateFlow(
        scope = viewModelScope + Dispatchers.IO,
        key = AppSettings.KEY_READER_SCREEN_ON,
        valueProducer = { isReaderKeepScreenOn },
    )

    val isWebtoonZooEnabled = observeIsWebtoonZoomEnabled()
        .stateIn(viewModelScope + Dispatchers.IO, SharingStarted.Lazily, false)

    val isWebtoonGapsEnabled = settings.observeAsStateFlow(
        scope = viewModelScope + Dispatchers.IO,
        key = AppSettings.KEY_WEBTOON_GAPS,
        valueProducer = { isWebtoonGapsEnabled },
    )

    val isWebtoonPullGestureEnabled = settings.observeAsStateFlow(
        scope = viewModelScope + Dispatchers.IO,
        key = AppSettings.KEY_WEBTOON_PULL_GESTURE,
        valueProducer = { isWebtoonPullGestureEnabled },
    )

    val defaultWebtoonZoomOut = observeIsWebtoonZoomEnabled().flatMapLatest {
        if (it) {
            observeWebtoonZoomOut()
        } else {
            flowOf(0f)
        }
    }.flowOn(Dispatchers.IO)

    val isZoomControlsEnabled = getObserveIsZoomControlEnabled().flatMapLatest { zoom ->
        if (zoom) {
            combine(readerMode, isWebtoonZooEnabled) { mode, ze -> ze || mode != ReaderMode.WEBTOON }
        } else {
            flowOf(false)
        }
    }.stateIn(viewModelScope + Dispatchers.IO, SharingStarted.Lazily, false)

    val readerSettingsProducer = readerSettingsProducerFactory.create(
        manga.mapNotNull { it?.id },
    )

    val isMangaNsfw = manga.map { it?.contentRating == ContentRating.ADULT }

    val isBookmarkAdded = readingState.flatMapLatest { state ->
        val manga = mangaDetails.value?.toManga()
        if (state == null || manga == null) {
            flowOf(false)
        } else {
            bookmarksRepository.observeBookmark(manga, state.chapterId, state.page)
                .map {
                    it != null && it.chapterId == state.chapterId && it.page == state.page
                }
        }
    }.stateIn(viewModelScope + Dispatchers.IO, SharingStarted.Eagerly, false)

    init {
        initIncognitoMode()
        loadImpl()
        launchJob(Dispatchers.IO) {
            val mangaId = manga.filterNotNull().first().id
            if (!isIncognitoMode.firstNotNull()) {
                appShortcutManager.notifyMangaOpened(mangaId)
            }
        }
    }

    fun reload() {
        loadingJob?.cancel()
        loadImpl()
    }

    fun onPause() {
        getMangaOrNull()?.let {
            statsCollector.onPause(it.id)
        }
    }

    fun onStop() {
        discordRpc.clearRpc()
    }

    fun onIdle() {
        discordRpc.setIdle()
    }

    fun switchMode(newMode: ReaderMode) {
        launchJob {
            val manga = checkNotNull(getMangaOrNull())
            dataRepository.saveReaderMode(
                manga = manga,
                mode = newMode,
            )
            readerMode.value = newMode
            content.update {
                it.copy(state = getCurrentState())
            }
        }
    }

    fun saveCurrentState(state: ReaderState? = null) {
        if (state != null) {
            readingState.value = state
            savedStateHandle[ReaderIntent.EXTRA_STATE] = state
        }
        if (isIncognitoMode.value != false) {
            return
        }
        val readerState = state ?: readingState.value ?: return
        historyUpdateUseCase.invokeAsync(
            manga = getMangaOrNull() ?: return,
            readerState = readerState,
            percent = computePercent(readerState.chapterId, readerState.page),
        )
    }

    fun getCurrentState() = readingState.value

    fun getCurrentChapterPages(): List<MangaPage>? {
        val chapterId = readingState.value?.chapterId ?: return null
        return chaptersLoader.getPages(chapterId.toChapterKey())
    }

    fun saveCurrentPage(
        pageSaveHelper: PageSaveHelper
    ) {
        val prevJob = pageSaveJob
        pageSaveJob = launchLoadingJob(Dispatchers.IO) {
            prevJob?.cancelAndJoin()
            val state = checkNotNull(getCurrentState())
            val currentManga = manga.requireValue()
            val task = PageSaveHelper.Task(
                manga = currentManga,
                chapterId = state.chapterId.toChapterKey(),
                pageNumber = state.page + 1,
                page = checkNotNull(getCurrentPage()) { "Cannot find current page" },
            )
            val dest = pageSaveHelper.save(setOf(task))
            onPageSaved.call(dest)
        }
    }

    fun getCurrentPage(): MangaPage? {
        val state = readingState.value ?: return null
        return content.value.pages.find {
            it.chapterId.toString() == state.chapterId && it.index == state.page
        }?.toMangaPage()
    }

    fun switchChapter(id: String, page: Int) {
        val prevJob = loadingJob
        loadingJob = launchLoadingJob(Dispatchers.IO) {
            prevJob?.cancelAndJoin()
            content.value = ReaderContent(emptyList(), null)
            chaptersLoader.loadSingleChapter(id.toChapterKey())
            val newState = ReaderState(id, page, 0)
            content.value = ReaderContent(chaptersLoader.snapshot(), newState)
            saveCurrentState(newState)
        }
    }

    fun switchChapterBy(delta: Int) {
        val prevJob = loadingJob
        loadingJob = launchLoadingJob(Dispatchers.IO) {
            prevJob?.cancelAndJoin()
            val prevState = readingState.requireValue()
            val newChapterId = if (delta != 0) {
                val allChapters = mangaDetails.requireValue().readerChaptersFor(prevState.chapterId)
                var index = allChapters.indexOfFirst { x -> x.id == prevState.chapterId }
                if (index < 0) {
                    return@launchLoadingJob
                }
                index += delta
                (allChapters.getOrNull(index) ?: return@launchLoadingJob).id
            } else {
                prevState.chapterId
            }
            content.value = ReaderContent(emptyList(), null)
            chaptersLoader.loadSingleChapter(newChapterId.toChapterKey())
            val newState = ReaderState(
                chapterId = newChapterId,
                page = if (delta == 0) prevState.page else 0,
                scroll = if (delta == 0) prevState.scroll else 0,
            )
            content.value = ReaderContent(chaptersLoader.snapshot(), newState)
            saveCurrentState(newState)
        }
    }

    @MainThread
    fun onCurrentPageChanged(lowerPos: Int, upperPos: Int) {
        val prevJob = stateChangeJob
        val pages = content.value.pages // capture immediately
        stateChangeJob = launchJob(Dispatchers.IO) {
            prevJob?.cancelAndJoin()
            loadingJob?.join()
            if (pages.size != content.value.pages.size) {
                return@launchJob // TODO
            }
            val centerPos = (lowerPos + upperPos) / 2
            pages.getOrNull(centerPos)?.let { page ->
                readingState.update { cs ->
                    cs?.copy(chapterId = page.chapterId.toString(), page = page.index)
                }
                pretranslateNextChapters(page.chapterId.toString())
            }
            notifyStateChanged()
            if (pages.isEmpty() || loadingJob?.isActive == true) {
                return@launchJob
            }
            ensureActive()
            val autoLoadAllowed = readerMode.value != ReaderMode.WEBTOON || !isWebtoonPullGestureEnabled.value
            if (autoLoadAllowed) {
                if (upperPos >= pages.lastIndex - BOUNDS_PAGE_OFFSET) {
                    loadPrevNextChapter(pages.last().chapterId.toString(), isNext = true)
                }
                if (lowerPos <= BOUNDS_PAGE_OFFSET) {
                    loadPrevNextChapter(pages.first().chapterId.toString(), isNext = false)
                }
            }
            if (pageLoader.isPrefetchApplicable()) {
                pageLoader.prefetch(pages.trySublist(upperPos + 1, upperPos + PREFETCH_LIMIT))
            }
        }
    }

    fun toggleBookmark() {
        if (bookmarkJob?.isActive == true) {
            return
        }
        bookmarkJob = launchJob(Dispatchers.IO) {
            loadingJob?.join()
            val state = checkNotNull(getCurrentState())
            if (isBookmarkAdded.value) {
                val manga = requireManga()
                bookmarksRepository.removeBookmark(manga.id, state.chapterId, state.page)
                onShowToast.call(R.string.bookmark_removed)
            } else {
                val page = checkNotNull(getCurrentPage()) { "Page not found" }
                val bookmark = Bookmark(
                    manga = requireManga(),
                    pageId = page.id,
                    chapterId = state.chapterId,
                    page = state.page,
                    scroll = state.scroll,
                    imageUrl = page.url,
                    createdAt = Instant.now(),
                    percent = computePercent(state.chapterId, state.page),
                )
                bookmarksRepository.addBookmark(bookmark)
                onShowToast.call(R.string.bookmark_added)
            }
        }
    }

    fun setIncognitoMode(value: Boolean, dontAskAgain: Boolean) {
        isIncognitoMode.value = value
        if (dontAskAgain) {
            settings.incognitoModeForNsfw = if (value) TriStateOption.ENABLED else TriStateOption.DISABLED
        }
    }

    fun updateReadingProgress() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                manga.collectLatest {
                    if (it != null) {
                        progressUpdateUseCase(it)
                    }
                }

                pageLoader.updateCache(getCurrentPage()!!)
            }
        }
    }

    private fun loadImpl() {
        loadingJob = launchLoadingJob(Dispatchers.IO + EventExceptionHandler(onLoadingError)) {
            var exception: Exception? = null
            var loadedDetails: MangaDetails? = null
            try {
                detailsLoadUseCase(intent, force = false)
                    .collect { details ->
                        loadedDetails = details
                        if (mangaDetails.value == null) {
                            mangaDetails.value = details
                        }
                        chaptersLoader.init(details)
                        val manga = details.toManga()
                        // obtain state
                        if (readingState.value == null) {
                            val newState = getStateFromIntent(manga)
                            if (newState == null) {
                                return@collect // manga not loaded yet if cannot get state
                            }
                            readingState.value = newState
                            val mode = runCatchingCancellable {
                                detectReaderModeUseCase(manga, newState)
                            }.getOrDefault(settings.defaultReaderMode)
                            val branch = chaptersLoader.peekChapter(newState.chapterId.toChapterKey())?.branch
                            selectedBranch.value = branch
                            readerMode.value = mode
                            try {
                                chaptersLoader.loadSingleChapter(newState.chapterId.toChapterKey())
                            } catch (e: Exception) {
                                readingState.value = null // try next time
                                exception = e.mergeWith(exception)
                                return@collect
                            }
                        }
                        mangaDetails.value = details.filterChapters(selectedBranch.value)

                        // save state
                        if (!isIncognitoMode.firstNotNull()) {
                            readingState.value?.let {
                                val percent = computePercent(it.chapterId, it.page)
                                historyUpdateUseCase(manga, it, percent)
                            }
                        }
                        notifyStateChanged()
                        content.value = ReaderContent(chaptersLoader.snapshot(), readingState.value)
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                exception = e.mergeWith(exception)
            }
            if (readingState.value == null) {
                val loadedManga = loadedDetails // for smart cast
                if (loadedManga != null) {
                    mangaDetails.value = loadedManga.filterChapters(selectedBranch.value)
                }
                val loadingError = when {
                    exception != null -> exception
                    loadedManga == null || !loadedManga.isLoaded -> null
                    loadedManga.isRestricted -> EmptyMangaException(
                        EmptyMangaReason.RESTRICTED,
                        loadedManga.toManga(),
                        null,
                    )

                    loadedManga.allChapters.isEmpty() -> EmptyMangaException(
                        EmptyMangaReason.NO_CHAPTERS,
                        loadedManga.toManga(),
                        null,
                    )

                    else -> null
                } ?: IllegalStateException("Unable to load manga. This should never happen. Please report")
                onLoadingError.call(loadingError)
            } else exception?.let { e ->
                // manga has been loaded but error occurred
                errorEvent.call(e)
            }
        }
    }

    @AnyThread
    private fun loadPrevNextChapter(currentId: String, isNext: Boolean) {
        val prevJob = loadingJob
        loadingJob = launchLoadingJob(Dispatchers.IO) {
            prevJob?.join()
            chaptersLoader.loadPrevNextChapter(mangaDetails.requireValue(), currentId.toChapterKey(), isNext)
            content.value = ReaderContent(chaptersLoader.snapshot(), null)
        }
    }

    private fun <T> List<T>.trySublist(fromIndex: Int, toIndex: Int): List<T> {
        val fromIndexBounded = fromIndex.coerceAtMost(lastIndex)
        val toIndexBounded = toIndex.coerceIn(fromIndexBounded, lastIndex)
        return if (fromIndexBounded == toIndexBounded) {
            emptyList()
        } else {
            subList(fromIndexBounded, toIndexBounded)
        }
    }

    @WorkerThread
    private fun notifyStateChanged() {
        val state = getCurrentState() ?: return
        val chapter = chaptersLoader.peekChapter(state.chapterId.toChapterKey()) ?: return
        val m = mangaDetails.value ?: return
        val orderedChapters = m.readerChapters(chapter.branch)
        val chapterIndex = orderedChapters.indexOfFirst { it.id == chapter.id }
        val newState = ReaderUiState(
            mangaName = m.toManga().title,
            chapter = chapter,
            chapterIndex = chapterIndex,
            chaptersTotal = orderedChapters.size,
            totalPages = chaptersLoader.getPagesCount(chapter.id.toChapterKey()),
            currentPage = state.page,
            percent = computePercent(state.chapterId, state.page),
            incognito = isIncognitoMode.value == true,
        )
        uiState.value = newState
        if (isIncognitoMode.value == false) {
            statsCollector.onStateChanged(m.id.toString(), state)
            discordRpc.updateRpc(m.toManga(), newState)
        }
    }

    private fun computePercent(chapterId: String, pageIndex: Int): Float {
        val branch = chaptersLoader.peekChapter(chapterId.toChapterKey())?.branch
        val chapters = mangaDetails.value?.readerChapters(branch) ?: return PROGRESS_NONE
        val chaptersCount = chapters.size
        val chapterIndex = chapters.indexOfFirst { x -> x.id == chapterId }
        val pagesCount = chaptersLoader.getPagesCount(chapterId.toChapterKey())
        if (chaptersCount == 0 || chapterIndex < 0 || pagesCount == 0) {
            return PROGRESS_NONE
        }
        val pagePercent = (pageIndex + 1) / pagesCount.toFloat()
        val ppc = 1f / chaptersCount
        return ppc * chapterIndex + ppc * pagePercent
    }

    private fun observeIsWebtoonZoomEnabled() = settings.observeAsFlow(
        key = AppSettings.KEY_WEBTOON_ZOOM,
        valueProducer = { isWebtoonZoomEnabled },
    )

    private fun observeWebtoonZoomOut() = settings.observeAsFlow(
        key = AppSettings.KEY_WEBTOON_ZOOM_OUT,
        valueProducer = { defaultWebtoonZoomOut },
    )

    private fun getObserveIsZoomControlEnabled() = settings.observeAsFlow(
        key = AppSettings.KEY_READER_ZOOM_BUTTONS,
        valueProducer = { isReaderZoomButtonsEnabled },
    )

    private fun initIncognitoMode() {
        if (isIncognitoMode.value != null) {
            return
        }
        launchJob(Dispatchers.IO) {
            interactor.observeIncognitoMode(manga)
                .collect {
                    when (it) {
                        TriStateOption.ENABLED -> isIncognitoMode.value = true
                        TriStateOption.ASK -> {
                            onAskNsfwIncognito.call(Unit)
                            return@collect
                        }

                        TriStateOption.DISABLED -> isIncognitoMode.value = false
                    }
                }
        }
    }

    private suspend fun getStateFromIntent(manga: Manga): ReaderState? {
        // check if we have at least some chapters loaded
        if (manga.chapters.isNullOrEmpty()) {
            return null
        }
        // specific state is requested
        val requestedState: ReaderState? = savedStateHandle[ReaderIntent.EXTRA_STATE]
        if (requestedState != null) {
            return if (manga.chapters?.find { it.id == requestedState.chapterId } != null) {
                requestedState
            } else {
                null
            }
        }

        val requestedBranch: String? = savedStateHandle[ReaderIntent.EXTRA_BRANCH]
        // continue reading
        val history = historyRepository.getOne(manga)
        if (history != null) {
            val chapter = manga.chapters?.find { it.id == history.chapterId } ?: return null
            // specified branch is requested
            return if (ReaderIntent.EXTRA_BRANCH in savedStateHandle) {
                if (chapter.branch == requestedBranch) {
                    ReaderState(history)
                } else {
                    ReaderState(manga, requestedBranch)
                }
            } else {
                ReaderState(history)
            }
        }

        // start from beginning
        val preferredBranch = requestedBranch ?: manga.getPreferredBranch(null)
        return ReaderState(manga, preferredBranch)
    }

    private fun Exception.mergeWith(other: Exception?): Exception = if (other == null) {
        this
    } else {
        other.addSuppressed(this)
        other
    }

    private fun pretranslateNextChapters(currentChapterId: String) {
        val mangaId = mangaDetails.value?.toManga()?.id ?: return
        if (!settings.isAiTranslateEnabled || !settings.isAiTranslateEnabledForManga(mangaId)) {
            return
        }
        val prevJob = pretranslationJob
        pretranslationJob = viewModelScope.launch(Dispatchers.IO) {
            prevJob?.cancel()
            
            val allChapters = mangaDetails.value?.readerChaptersFor(currentChapterId) ?: return@launch
            val currentIndex = allChapters.indexOfFirst { it.id == currentChapterId }
            if (currentIndex == -1) return@launch
            
            val nextChapters = allChapters.drop(currentIndex + 1).take(5)
            
            for (chapter in nextChapters) {
                ensureActive()
                val chapterId = chapter.id
                if (translator.hasChapterCached(chapterId.toChapterKey()) || !pretranslatingChapters.add(chapterId)) {
                    continue
                }
                
                try {
                    android.util.Log.d("MangaTranslator", "Pre-translating next chapter: id=${chapterId} title=${chapter.title}")
                    
                    val mangaSource = mangaDetails.value?.toManga()?.source?.name ?: return@launch
                    val repo = mangaRepositoryFactory.create(com.nyora.hasan72341.core.model.MangaSource(mangaSource))
                    val chapterPages = repo.getPages(chapter)
                    ensureActive()
                    
                    val uris = chapterPages.map { page ->
                        ensureActive()
                        pageLoader.loadPage(page, force = false)
                    }
                    ensureActive()
                    
                    val decodedUris = uris.map { uri ->
                        ensureActive()
                        pageLoader.convertBimap(uri)
                    }
                    ensureActive()
                    
                    translator.translateChapter(chapterId.toChapterKey(), decodedUris)
                    
                    android.util.Log.d("MangaTranslator", "Successfully pre-translated chapter: id=${chapterId}")
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    android.util.Log.e("MangaTranslator", "Failed to pre-translate chapter $chapterId", e)
                } finally {
                    pretranslatingChapters.remove(chapterId)
                }
            }
        }
    }
}
