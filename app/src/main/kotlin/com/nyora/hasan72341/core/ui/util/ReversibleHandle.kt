package com.nyora.hasan72341.core.ui.util

import com.nyora.hasan72341.core.util.ext.printStackTraceDebug
import com.nyora.hasan72341.core.util.ext.processLifecycleScope
import com.nyora.hasan72341.mihon.parsers.util.runCatchingCancellable
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun interface ReversibleHandle {

	suspend fun reverse()
}

fun ReversibleHandle.reverseAsync() = processLifecycleScope.launch(Dispatchers.IO, CoroutineStart.ATOMIC) {
	runCatchingCancellable {
		withContext(NonCancellable) {
			reverse()
		}
	}.onFailure {
		it.printStackTraceDebug("ReversibleHandle::reverseAsync")
	}
}
