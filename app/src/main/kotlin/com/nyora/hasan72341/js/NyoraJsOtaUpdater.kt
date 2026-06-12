package com.nyora.hasan72341.js

import android.content.Context
import android.util.Log
import com.nyora.hasan72341.core.network.MangaHttpClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NyoraJsOtaUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    @MangaHttpClient private val httpClient: OkHttpClient
) {
    /**
     * The version of parsers.bundle.js shipped in the app's assets.
     * Increment this whenever you manually update assets/parsers.bundle.js.
     */
    private val BUNDLED_VERSION = 36

    private val DEFAULT_MANIFEST_URL =
        "https://hasan72341.github.io/nyora-ota-parsers/manifest.json"

    private fun manifestUrl(): String {
        return System.getProperty("nyora.ota.manifest")
            ?: System.getenv("NYORA_OTA_MANIFEST")
            ?: DEFAULT_MANIFEST_URL
    }

    private fun otaDir(): File = File(context.filesDir, "ota")

    fun localVersion(): Int {
        val otaVersion = otaVersion()
        return if (hasCompleteOta() && otaVersion > BUNDLED_VERSION) otaVersion else BUNDLED_VERSION
    }

    private fun otaVersion(): Int {
        val versionFile = File(otaDir(), "version")
        if (!versionFile.exists()) return 0
        return runCatching { versionFile.readText().trim().toIntOrNull() }.getOrNull() ?: 0
    }

    private fun hasCompleteOta(): Boolean {
        val dir = otaDir()
        return File(dir, "parsers.bundle.js").exists() &&
            File(dir, "parsers_sources.json").exists()
    }

    fun isActive(): Boolean {
        // Only active if the OTA version is STRICTLY newer than the bundled assets.
        return hasCompleteOta() && otaVersion() > BUNDLED_VERSION
    }

    fun bundle(): String? =
        if (isActive()) runCatching { File(otaDir(), "parsers.bundle.js").readText() }.getOrNull() else null

    fun sources(): String? =
        if (isActive()) runCatching { File(otaDir(), "parsers_sources.json").readText() }.getOrNull() else null

    fun updateOnce(): Boolean {
        val url = manifestUrl()
        val sep = if (url.contains("?")) "&" else "?"
        val request = Request.Builder()
            .url("$url${sep}t=${System.currentTimeMillis()}")
            .header("Cache-Control", "no-cache")
            .build()
        val responseText = httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw RuntimeException("HTTP ${response.code}")
            response.body?.string().orEmpty()
        }
        val manifest = Json.parseToJsonElement(responseText).jsonObject
        val remoteVersion = manifest["version"]?.jsonPrimitive?.intOrNull ?: return false

        if (remoteVersion <= localVersion()) {
            Log.d("NyoraOTA", "Parsers up to date (remote v$remoteVersion, local v${localVersion()})")
            return false
        }

        val bundleObj = manifest["bundle"]?.jsonObject ?: return false
        val sourcesObj = manifest["sources"]?.jsonObject ?: return false

        // Download and verify bundle
        val bundleUrl = bundleObj["url"]!!.jsonPrimitive.content
        val bundleBytes = downloadBytes(bundleUrl)
        if (sha256(bundleBytes) != bundleObj["sha256"]?.jsonPrimitive?.content) {
            Log.w("NyoraOTA", "Bundle SHA-256 mismatch - ignoring update")
            return false
        }

        // Download and verify catalog
        val sourcesUrl = sourcesObj["url"]!!.jsonPrimitive.content
        val sourcesBytes = downloadBytes(sourcesUrl)
        if (sha256(sourcesBytes) != sourcesObj["sha256"]?.jsonPrimitive?.content) {
            Log.w("NyoraOTA", "Sources SHA-256 mismatch - ignoring update")
            return false
        }

        // Write files atomically
        val dir = otaDir()
        if (!dir.exists()) {
            dir.mkdirs()
        }

        writeAtomic(File(dir, "parsers.bundle.js"), bundleBytes)
        writeAtomic(File(dir, "parsers_sources.json"), sourcesBytes)
        writeAtomic(File(dir, "version"), remoteVersion.toString().toByteArray())
        Log.i("NyoraOTA", "Parser bundle v$remoteVersion downloaded - active on next launch")
        return true
    }

    private fun downloadBytes(url: String): ByteArray {
        val request = Request.Builder().url(url).build()
        return httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw RuntimeException("HTTP ${response.code} downloading $url")
            response.body?.bytes() ?: ByteArray(0)
        }
    }

    private fun writeAtomic(target: File, bytes: ByteArray) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        tmp.writeBytes(bytes)
        if (!tmp.renameTo(target)) {
            target.delete()
            tmp.renameTo(target)
        }
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
