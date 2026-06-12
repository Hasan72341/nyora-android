package com.nyora.hasan72341.core.network

import android.os.Build
import com.nyora.hasan72341.core.exceptions.CloudFlareBlockedException
import com.nyora.hasan72341.core.exceptions.CloudFlareProtectedException
import com.nyora.hasan72341.core.util.ext.printStackTraceDebug
import okhttp3.Interceptor
import okhttp3.Response
import okio.IOException
import com.nyora.hasan72341.mihon.parsers.model.MangaSource
import com.nyora.hasan72341.mihon.parsers.network.CloudFlareHelper

class CloudFlareInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        // Android 6 (SDK 23) has a bug in the implementation of the
        // "International Components for Unicode (ICU)" charset decoder used by "java.nio".
        // The error occurs when reading a stream into a String if the byte sequence
        // is interpreted in a way that causes the ICU decoder to flush its buffer incorrectly,
        // resulting in a negative position in the underlying "ByteBuffer".
        //
        // As a workaround, we need to manually decode the bytes to avoid the ICU "Bad position" bug
        // when running on Android 6.
        val protectionType = if (Build.VERSION.SDK_INT == 23) {
            try {
                // Peek up to 512 bytes safely.
                val bodyBytes = response.peekBody(512).bytes()
                val bodyString = String(bodyBytes, Charsets.UTF_8)

                when {
                    // Check for common Cloudflare challenge indicators
                    bodyString.contains("cf-challenge") ||
                        bodyString.contains("ray_id") ||
                        bodyString.contains("jschl_vc") -> {
                        CloudFlareHelper.PROTECTION_CAPTCHA
                    }
                    // Check for access denied/blocked
                    bodyString.contains("cf-error-details") -> {
                        CloudFlareHelper.PROTECTION_BLOCKED
                    }

                    else -> CloudFlareHelper.PROTECTION_NOT_DETECTED
                }
            } catch (e: Exception) {
                e.printStackTraceDebug("CloudFlareInterceptor")
                CloudFlareHelper.PROTECTION_NOT_DETECTED
            }
        } else {
            // Standard path for Android 7.0+
            try {
                CloudFlareHelper.checkResponseForProtection(response)
            } catch (e: IllegalArgumentException) {
                if (e.message?.contains("Bad position") == true) {
                    CloudFlareHelper.PROTECTION_NOT_DETECTED
                } else {
                    e.printStackTraceDebug("CloudFlareInterceptor")
                }
            }
        }

        // The bundled CloudFlareHelper only recognises the legacy challenge markers
        // (jschl_vc / cf-challenge). Modern Cloudflare "managed challenges" instead return a
        // 403/503 with a `cf-mitigated: challenge` header (or a challenges.cloudflare.com body),
        // which slipped through undetected — so searches/lists silently received the challenge
        // HTML instead of data. Treat those as a solvable captcha.
        val resolvedProtection = if (
            protectionType == CloudFlareHelper.PROTECTION_NOT_DETECTED &&
            response.isModernCloudFlareChallenge()
        ) {
            CloudFlareHelper.PROTECTION_CAPTCHA
        } else {
            protectionType
        }

        return when (resolvedProtection) {
            CloudFlareHelper.PROTECTION_BLOCKED -> response.closeThrowing(
                CloudFlareBlockedException(
                    url = request.url.toString(),
                    source = request.tag(MangaSource::class.java),
                ),
            )

            CloudFlareHelper.PROTECTION_CAPTCHA -> response.closeThrowing(
                CloudFlareProtectedException(
                    url = request.url.toString(),
                    source = request.tag(MangaSource::class.java),
                    headers = request.headers,
                ),
            )

            else -> response
        }
    }

    private fun Response.isModernCloudFlareChallenge(): Boolean {
        if (header("cf-mitigated").equals("challenge", ignoreCase = true)) {
            return true
        }
        if (code != 403 && code != 503) {
            return false
        }
        if (header("server")?.contains("cloudflare", ignoreCase = true) != true) {
            return false
        }
        val body = try {
            peekBody(MODERN_CHALLENGE_PEEK).string()
        } catch (e: Exception) {
            e.printStackTraceDebug("CloudFlareInterceptor::isModernCloudFlareChallenge")
            return false
        }
        return body.contains("challenges.cloudflare.com", ignoreCase = true) ||
            body.contains("Just a moment", ignoreCase = true) ||
            body.contains("__cf_chl", ignoreCase = true) ||
            body.contains("cf_chl_opt", ignoreCase = true)
    }

    private fun Response.closeThrowing(error: IOException): Nothing {
        try {
            close()
        } catch (e: Exception) {
            error.addSuppressed(e)
        }
        throw error
    }

    private companion object {
        private const val MODERN_CHALLENGE_PEEK = 64L * 1024L
    }
}
