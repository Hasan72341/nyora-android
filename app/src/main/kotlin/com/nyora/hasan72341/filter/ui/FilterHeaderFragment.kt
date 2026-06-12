package com.nyora.hasan72341.filter.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import com.nyora.hasan72341.core.nav.router
import com.nyora.hasan72341.core.ui.BaseFragment
import com.nyora.hasan72341.core.ui.widgets.ChipsView
import com.nyora.hasan72341.core.util.ext.isAnimationsEnabled
import com.nyora.hasan72341.core.util.ext.observe
import com.nyora.hasan72341.databinding.FragmentFilterHeaderBinding
import com.nyora.hasan72341.filter.data.PersistableFilter
import com.nyora.hasan72341.filter.ui.model.FilterHeaderModel
import com.nyora.hasan72341.mihon.parsers.model.ContentRating
import com.nyora.hasan72341.mihon.parsers.model.ContentType
import com.nyora.hasan72341.mihon.parsers.model.Demographic
import com.nyora.hasan72341.mihon.parsers.model.MangaState
import com.nyora.hasan72341.mihon.parsers.model.MangaTag
import com.nyora.hasan72341.mihon.parsers.model.YEAR_UNKNOWN
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class FilterHeaderFragment : BaseFragment<FragmentFilterHeaderBinding>(), ChipsView.OnChipClickListener,
    ChipsView.OnChipCloseClickListener {

    @Inject
    lateinit var filterHeaderProducer: FilterHeaderProducer

    private val filter: FilterCoordinator
        get() = (requireActivity() as FilterCoordinator.Owner).filterCoordinator

    override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentFilterHeaderBinding {
        return FragmentFilterHeaderBinding.inflate(inflater, container, false)
    }

    override fun onViewBindingCreated(binding: FragmentFilterHeaderBinding, savedInstanceState: Bundle?) {
        super.onViewBindingCreated(binding, savedInstanceState)
        binding.chipsTags.onChipClickListener = this
        binding.chipsTags.onChipCloseClickListener = this
        filterHeaderProducer.observeHeader(filter)
            .flowOn(Dispatchers.IO)
            .observe(viewLifecycleOwner, ::onDataChanged)
    }

    override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat = insets

    override fun onChipClick(chip: Chip, data: Any?) {
        when (data) {
            is MangaTag -> filter.toggleTag(data, !chip.isChecked)
            is PersistableFilter -> if (chip.isChecked) {
                filter.reset()
            } else {
                filter.setAdjusted(data.filter)
            }

            is String -> Unit
            null -> router.showTagsCatalogSheet(excludeMode = false)
        }
    }

    override fun onChipCloseClick(chip: Chip, data: Any?) {
        when (data) {
            is String -> if (data == filter.snapshot().listFilter.author) {
                filter.setAuthor(null)
            } else {
                filter.setQuery(null)
            }

            is ContentRating -> filter.toggleContentRating(data, false)
            is Demographic -> filter.toggleDemographic(data, false)
            is ContentType -> filter.toggleContentType(data, false)
            is MangaState -> filter.toggleState(data, false)
            is Locale -> filter.setLocale(null)
            is Int -> filter.setYear(YEAR_UNKNOWN)
            is IntRange -> filter.setYearRange(YEAR_UNKNOWN, YEAR_UNKNOWN)
        }
    }

    private fun onDataChanged(header: FilterHeaderModel) {
        val binding = viewBinding ?: return
        val chips = header.chips
        if (chips.isEmpty()) {
            binding.chipsTags.setChips(emptyList())
            binding.root.isVisible = false
            return
        }
        binding.chipsTags.setChips(header.chips)
        binding.root.isVisible = true
        if (binding.root.context.isAnimationsEnabled) {
            binding.scrollView.smoothScrollTo(0, 0)
        } else {
            binding.scrollView.scrollTo(0, 0)
        }
    }
}
