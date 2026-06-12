package com.nyora.hasan72341.details.domain

import com.nyora.hasan72341.core.util.LocaleStringComparator
import com.nyora.hasan72341.details.ui.model.MangaBranch

class BranchComparator : Comparator<MangaBranch> {

	private val delegate = LocaleStringComparator()

	override fun compare(o1: MangaBranch, o2: MangaBranch): Int = delegate.compare(o1.name, o2.name)
}
