package com.nyora.hasan72341.core.network.webview

import android.graphics.Bitmap
import android.webkit.WebView
import com.nyora.hasan72341.core.network.cookies.MutableCookieJar
import com.nyora.hasan72341.mihon.parsers.network.CloudFlareHelper
import kotlin.coroutines.Continuation

class CaptchaContinuationClient(
    private val cookieJar: MutableCookieJar,
    private val targetUrl: String,
    continuation: Continuation<Unit>,
) : ContinuationResumeWebViewClient(continuation) {

    private val oldClearance = CloudFlareHelper.getClearanceCookie(cookieJar, targetUrl)

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        checkClearance(view)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        checkClearance(view)
    }

    private fun checkClearance(view: WebView?) {
        val clearance = CloudFlareHelper.getClearanceCookie(cookieJar, targetUrl)
        if (clearance != null && clearance != oldClearance) {
            resumeContinuation(view)
        }
    }
}
