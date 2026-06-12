package com.nyora.hasan72341.core.util

import com.nyora.hasan72341.core.util.ext.printStackTraceDebug
import com.nyora.hasan72341.core.util.ext.report
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class AcraCoroutineErrorHandler : AbstractCoroutineContextElement(CoroutineExceptionHandler),
	CoroutineExceptionHandler {

	override fun handleException(context: CoroutineContext, exception: Throwable) {
		exception.printStackTraceDebug("AcraCoroutineErrorHandler::handleException")
		exception.report()
	}
}
