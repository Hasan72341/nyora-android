package com.nyora.hasan72341.tachiyomi.network

/**
 * Progress listener interface for tracking download progress.
 */
interface ProgressListener {
    fun update(bytesRead: Long, contentLength: Long, done: Boolean)
}
