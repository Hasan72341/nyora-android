package com.nyora.hasan72341.core.parser.datadriven

import android.content.Context
import app.nyora.data.engine.EngineRegistry
import com.nyora.hasan72341.core.model.DataDrivenMangaSource
import com.nyora.hasan72341.core.network.MangaHttpClient
import com.nyora.hasan72341.core.util.ext.printStackTraceDebug
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches and caches the runtime source catalogue — the list of DATA-only sources rendered by the
 * bundled generic engines, instead of a per-source parser catalogue compiled into the APK. The
 * manifest (nyora-data-driven's catalogue.json) is fetched over the shared manga client, persisted
 * to disk so the catalogue survives offline, and turned into [DataDrivenMangaSource]s. Rows whose
 * engine this build doesn't bundle, or that are flagged broken, are dropped.
 *
 * [sources] reads the in-memory cache synchronously (populated from disk on first use) so the source
 * list can render immediately; [refresh] updates it from the network.
 */
@Singleton
class DataDrivenCatalogueRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @MangaHttpClient private val okHttpClient: OkHttpClient,
    private val settings: com.nyora.hasan72341.core.prefs.AppSettings,
) {

    private val cacheFile = File(context.filesDir, CACHE_FILE)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Volatile
    private var cached: List<DataDrivenMangaSource>? = null

    init {
        // Load the disk cache eagerly so the source list has the catalogue synchronously at startup
        // (before the async refresh), rather than only after a relaunch.
        loadFromDisk().takeIf { it.isNotEmpty() }?.let { cached = it }
    }

    /** The currently known data-driven sources (disk cache until a [refresh] lands). */
    val sources: List<DataDrivenMangaSource>
        // Never cache an EMPTY disk read: on a first-ever launch the disk cache doesn't exist yet, and
        // caching the empty result would pin it and starve the source list even after refresh() lands.
        get() = cached ?: loadFromDisk().also { if (it.isNotEmpty()) cached = it }

    /** The catalogue URL the user configured, or a debug-only default so dev builds work offline. */
    val catalogueUrl: String
        get() = settings.sourceCatalogueUrl.ifBlank {
            if (com.nyora.hasan72341.BuildConfig.DEBUG) DEBUG_DEFAULT_URL else ""
        }

    /**
     * Fetch the latest catalogue from the user-configured URL, replacing the cache. Returns the
     * parsed source count, or 0 (success) when no URL is configured yet — the app ships with no
     * baked-in catalogue, so there is simply nothing to fetch until the user pastes a repository.
     */
    suspend fun refresh(): Result<Int> = withContext(Dispatchers.IO) {
        val url = catalogueUrl
        if (url.isBlank()) {
            return@withContext Result.success(0)
        }
        runCatching {
            val request = Request.Builder().url(url).build()
            val body = okHttpClient.newCall(request).execute().use { resp ->
                require(resp.isSuccessful) { "catalogue fetch failed: HTTP ${resp.code}" }
                // Bounded read: never buffer an unexpectedly huge/hostile response into memory. The
                // real catalogue is well under this; a larger body is truncated and fails to parse
                // (caught below) rather than risking an OOM.
                resp.peekBody(MAX_CATALOGUE_BYTES).string()
            }
            val parsed = parse(body)
            runCatching { cacheFile.writeText(body) } // cache best-effort; a write failure isn't fatal
            publish(parsed)
            parsed.size
        }.onFailure { it.printStackTraceDebug("DataDrivenCatalogueRepository") }
    }

    private fun loadFromDisk(): List<DataDrivenMangaSource> =
        runCatching { if (cacheFile.exists()) parse(cacheFile.readText()) else emptyList() }
            .getOrDefault(emptyList())
            .also { DataDrivenMangaSource.register(it) }

    private fun publish(sources: List<DataDrivenMangaSource>) {
        cached = sources
        DataDrivenMangaSource.register(sources)
    }

    private fun parse(text: String): List<DataDrivenMangaSource> {
        // Decode each row independently so a single malformed/forward-incompatible entry is skipped
        // rather than discarding the whole catalogue.
        val rows = json.parseToJsonElement(text).jsonObject["sources"]?.jsonArray.orEmpty()
        return rows.asSequence()
            .mapNotNull { runCatching { json.decodeFromJsonElement<SourceRowDto>(it) }.getOrNull() }
            // Drop only DEAD sources (a brokenReason is set on genuinely-unavailable ones: domain
            // doesn't resolve, sold/parked, dead upstream). A bare `broken` flag with no reason is the
            // kotatsu extraction's @Broken marker — it means the NATIVE kotatsu parser was broken,
            // which is irrelevant here since data-driven sources use our own engines, so those are kept.
            .filterNot { it.broken && !it.brokenReason.isNullOrBlank() }
            // Drop rows that can't yield a usable source: no id/domain, or an engine this build can't
            // render — either a bundled generic engine (EngineRegistry) or a natively-backed engine
            // whose repository lives in the app (e.g. mangafire: custom JSON API + image scrambling
            // a generic engine can't express).
            .filter {
                it.id.isNotBlank() && it.domain.isNotBlank() &&
                    (EngineRegistry.supports(it.engine) || it.engine in NATIVE_BACKED_ENGINES)
            }
            .map { row ->
                val config = row.config.toValueMap().toMutableMap()
                // Surface the row-level pageSize into the config the engine reads.
                if (row.pageSize != null) config.putIfAbsent("pageSize", row.pageSize)
                DataDrivenMangaSource(
                    sourceId = row.id,
                    engineKey = row.engine,
                    title = row.name.ifEmpty { row.id },
                    lang = row.lang,
                    nsfw = row.nsfw,
                    domain = row.domain.sanitizeDomain(),
                    contentType = row.contentType,
                    config = config,
                )
            }
            .filter { it.domain.isNotBlank() }
            .toList()
    }

    @Serializable
    private data class SourceRowDto(
        val id: String,
        val name: String = "",
        val lang: String = "en",
        val nsfw: Boolean = false,
        val contentType: String? = null,
        val engine: String,
        val domain: String,
        val broken: Boolean = false,
        val brokenReason: String? = null,
        val pageSize: Int? = null,
        val config: JsonObject = JsonObject(emptyMap()),
    )

    private fun JsonObject.toValueMap(): Map<String, Any?> = mapValues { it.value.toValue() }

    private fun JsonElement.toValue(): Any? = when (this) {
        is JsonNull -> null
        is JsonPrimitive -> when {
            isString -> content
            booleanOrNull != null -> booleanOrNull
            longOrNull != null -> longOrNull!!.let { if (it in Int.MIN_VALUE..Int.MAX_VALUE) it.toInt() else it }
            doubleOrNull != null -> doubleOrNull
            else -> content
        }
        is JsonObject -> toValueMap()
        is JsonArray -> map { it.toValue() }
    }

    // Reduce a catalogue `domain` to a bare host, so the engines' "https://{domain}/…" URL building
    // can never be derailed by a stray scheme, path, query or whitespace in the data.
    private fun String.sanitizeDomain(): String = trim()
        .substringAfter("://")
        .substringBefore('/')
        .substringBefore('?')
        .trim()

    companion object {
        // Engines with no generic implementation that are instead handled by a bundled native
        // MangaRepository (routed in MangaRepository.Factory). Kept in the catalogue so those
        // sources still appear, filter, and categorise like any other data-driven source.
        private val NATIVE_BACKED_ENGINES = setOf("mangafire")
        private const val CACHE_FILE = "datadriven-catalogue.json"
        private const val MAX_CATALOGUE_BYTES = 16L * 1024 * 1024 // 16 MiB; real catalogue is ~350 KiB
        // Debug-only convenience default so dev builds have sources without pasting a URL each install.
        // Guarded by BuildConfig.DEBUG, so R8 strips it (and this string) from the release APK — the
        // store build carries no catalogue URL and no source domains until the user configures one.
        private const val DEBUG_DEFAULT_URL =
            "https://raw.githubusercontent.com/Nyora-Manga/nyora-data-driven/main/catalogue.json"
    }
}
