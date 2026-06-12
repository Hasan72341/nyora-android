package com.nyora.hasan72341.details.ui.pager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ActionMode
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import com.nyora.hasan72341.core.exceptions.resolve.SnackbarErrorObserver
import com.nyora.hasan72341.core.nav.AppRouter
import com.nyora.hasan72341.core.nav.router
import com.nyora.hasan72341.core.prefs.AppSettings
import com.nyora.hasan72341.core.ui.sheet.AdaptiveSheetBehavior.Companion.STATE_COLLAPSED
import com.nyora.hasan72341.core.ui.sheet.AdaptiveSheetBehavior.Companion.STATE_DRAGGING
import com.nyora.hasan72341.core.ui.sheet.AdaptiveSheetBehavior.Companion.STATE_EXPANDED
import com.nyora.hasan72341.core.ui.sheet.AdaptiveSheetBehavior.Companion.STATE_SETTLING
import com.nyora.hasan72341.core.ui.sheet.AdaptiveSheetCallback
import com.nyora.hasan72341.core.ui.sheet.BaseAdaptiveSheet
import com.nyora.hasan72341.core.ui.util.ActionModeListener
import com.nyora.hasan72341.core.ui.util.MenuInvalidator
import com.nyora.hasan72341.core.ui.util.RecyclerViewOwner
import com.nyora.hasan72341.core.ui.util.ReversibleActionObserver
import com.nyora.hasan72341.core.util.ext.doOnPageChanged
import com.nyora.hasan72341.core.util.ext.findCurrentPagerFragment
import com.nyora.hasan72341.core.util.ext.menuView
import com.nyora.hasan72341.core.util.ext.observe
import com.nyora.hasan72341.core.util.ext.observeEvent
import com.nyora.hasan72341.core.util.ext.recyclerView
import com.nyora.hasan72341.core.util.ext.smoothScrollToTop
import com.nyora.hasan72341.databinding.SheetChaptersPagesBinding
import com.nyora.hasan72341.details.ui.DetailsViewModel
import com.nyora.hasan72341.details.ui.ReadButtonDelegate
import com.nyora.hasan72341.download.ui.worker.DownloadStartedObserver
import javax.inject.Inject

@AndroidEntryPoint
class ChaptersPagesSheet : BaseAdaptiveSheet<SheetChaptersPagesBinding>(),
	TabLayout.OnTabSelectedListener,
	ActionModeListener,
	AdaptiveSheetCallback {

	@Inject
	lateinit var settings: AppSettings

	private val viewModel by ChaptersPagesViewModel.ActivityVMLazy(this)

	override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup?): SheetChaptersPagesBinding {
		return SheetChaptersPagesBinding.inflate(inflater, container, false)
	}

	override fun onViewBindingCreated(binding: SheetChaptersPagesBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		disableFitToContents()

		val args = arguments ?: Bundle.EMPTY
		var defaultTab = args.getInt(AppRouter.KEY_TAB, settings.defaultDetailsTab)
		val adapter = ChaptersPagesAdapter(this, settings.isPagesTabEnabled)
		if (!adapter.isPagesTabEnabled) {
			defaultTab = (defaultTab - 1).coerceAtLeast(TAB_CHAPTERS)
		}
		(viewModel as? DetailsViewModel)?.let { dvm ->
			ReadButtonDelegate(binding.splitButtonRead, dvm, router).attach(viewLifecycleOwner)
		}
		binding.pager.offscreenPageLimit = adapter.itemCount
		binding.pager.recyclerView?.isNestedScrollingEnabled = false
		binding.pager.adapter = adapter
		binding.pager.doOnPageChanged(::onPageChanged)
		TabLayoutMediator(binding.tabs, binding.pager, adapter).attach()
		binding.tabs.addOnTabSelectedListener(this)
		binding.pager.setCurrentItem(defaultTab, false)
		binding.tabs.isVisible = adapter.itemCount > 1

		val menuProvider = ChapterPagesMenuProvider(viewModel, this, binding.pager, settings)
		onBackPressedDispatcher.addCallback(viewLifecycleOwner, menuProvider)
		binding.toolbar.addMenuProvider(menuProvider)

		val menuInvalidator = MenuInvalidator(binding.toolbar)
		viewModel.isChaptersReversed.observe(viewLifecycleOwner, menuInvalidator)
		viewModel.isChaptersInGridView.observe(viewLifecycleOwner, menuInvalidator)
		viewModel.isDownloadedOnly.observe(viewLifecycleOwner, menuInvalidator)

		actionModeDelegate?.addListener(this, viewLifecycleOwner)
		addSheetCallback(this, viewLifecycleOwner)

		viewModel.newChaptersCount.observe(viewLifecycleOwner, ::onNewChaptersChanged)
		if (dialog != null) {
			viewModel.onError.observeEvent(viewLifecycleOwner, SnackbarErrorObserver(binding.pager, this))
			viewModel.onActionDone.observeEvent(viewLifecycleOwner, ReversibleActionObserver(binding.pager))
			viewModel.onDownloadStarted.observeEvent(viewLifecycleOwner, DownloadStartedObserver(binding.pager))
		} else {
			PeekHeightController(arrayOf(binding.headerBar, binding.toolbar)).attach()
		}
	}

	override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat = insets

	override fun onStateChanged(sheet: View, newState: Int) {
        val binding = viewBinding ?: return
        binding.layoutTouchBlock.isTouchEventsAllowed = dialog != null || newState != STATE_COLLAPSED
        if (newState == STATE_DRAGGING || newState == STATE_SETTLING) {
            return
        }
		val isActionModeStarted = actionModeDelegate?.isActionModeStarted == true
		binding.toolbar.menuView?.isVisible = newState == STATE_EXPANDED && !isActionModeStarted
		binding.splitButtonRead.isVisible = newState != STATE_EXPANDED && !isActionModeStarted
			&& viewModel is DetailsViewModel
	}

	override fun onActionModeStarted(mode: ActionMode) {
		viewBinding?.toolbar?.menuView?.isVisible = false
		view?.post(::expandAndLock)
	}

	override fun onActionModeFinished(mode: ActionMode) {
		unlock()
		val state = behavior?.state ?: STATE_EXPANDED
		viewBinding?.toolbar?.menuView?.isVisible = state != STATE_COLLAPSED
	}

	override fun onTabSelected(tab: TabLayout.Tab?) = Unit

	override fun onTabUnselected(tab: TabLayout.Tab?) = Unit

	override fun onTabReselected(tab: TabLayout.Tab?) {
		val f = childFragmentManager.findCurrentPagerFragment(
			viewBinding?.pager ?: return,
		) as? RecyclerViewOwner ?: return
		f.recyclerView?.smoothScrollToTop()
	}

	override fun expandAndLock() {
		super.expandAndLock()
		adjustLockState()
	}

	override fun unlock() {
		super.unlock()
		adjustLockState()
	}

	private fun adjustLockState() {
		viewBinding?.run {
			pager.isUserInputEnabled = !isLocked
			tabs.visibility = when {
				(pager.adapter?.itemCount ?: 0) <= 1 -> View.GONE
				isLocked -> View.INVISIBLE
				else -> View.VISIBLE
			}
		}
	}

	private fun onPageChanged(position: Int) {
		viewBinding?.toolbar?.invalidateMenu()
		settings.lastDetailsTab = position
	}

	private fun onNewChaptersChanged(counter: Int) {
		val tab = viewBinding?.tabs?.getTabAt(0) ?: return
		if (counter == 0) {
			tab.removeBadge()
		} else {
			val badge = tab.orCreateBadge
			badge.number = counter
		}
	}

	companion object {

		const val TAB_CHAPTERS = 0
		const val TAB_PAGES = 1
		const val TAB_BOOKMARKS = 2
	}
}
