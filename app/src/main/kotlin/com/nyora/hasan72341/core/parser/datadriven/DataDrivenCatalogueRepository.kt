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
 * Fetches, caches and parses the runtime source catalogue (catalogue.json) into
 * [DataDrivenMangaSource]s. [sources] reads the disk/memory cache; [refresh] updates from the network.
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
        // Eager disk load so the source list has the catalogue at startup, before the async refresh.
        loadFromDisk().takeIf { it.isNotEmpty() }?.let { cached = it }
    }

    val sources: List<DataDrivenMangaSource>
        // Don't cache an empty disk read; it would pin the empty list even after refresh() lands.
        get() = cached ?: loadFromDisk().also { if (it.isNotEmpty()) cached = it }

    val catalogueUrl: String
        get() = settings.sourceCatalogueUrl.ifBlank {
            if (com.nyora.hasan72341.BuildConfig.DEBUG) DEBUG_DEFAULT_URL else ""
        }

    /** Refresh from [catalogueUrl]. Returns the parsed source count, or 0 when no URL is set yet. */
    suspend fun refresh(): Result<Int> = withContext(Dispatchers.IO) {
        val url = catalogueUrl
        if (url.isBlank()) {
            return@withContext Result.success(0)
        }
        runCatching {
            val request = Request.Builder().url(url).build()
            val body = okHttpClient.newCall(request).execute().use { resp ->
                require(resp.isSuccessful) { "catalogue fetch failed: HTTP ${resp.code}" }
                resp.peekBody(MAX_CATALOGUE_BYTES).string()
            }
            val parsed = parse(body)
            runCatching { cacheFile.writeText(body) }
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
        // Decode each row independently so one bad entry doesn't discard the whole catalogue.
        val rows = json.parseToJsonElement(text).jsonObject["sources"]?.jsonArray.orEmpty()
        return rows.asSequence()
            .mapNotNull { runCatching { json.decodeFromJsonElement<SourceRowDto>(it) }.getOrNull() }
            // Keep bare @Broken rows (that flag is about the native kotatsu parser, not our engines);
            // drop only rows with an explicit brokenReason (dead domain).
            .filterNot { it.broken && !it.brokenReason.isNullOrBlank() }
            .filter {
                it.id.isNotBlank() && it.domain.isNotBlank() &&
                    (EngineRegistry.supports(it.engine) || it.engine in NATIVE_BACKED_ENGINES)
            }
            .map { row ->
                val config = row.config.toValueMap().toMutableMap()
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

    // Strip any scheme/path/query so URL building can't be derailed by stray data.
    private fun String.sanitizeDomain(): String = trim()
        .substringAfter("://")
        .substringBefore('/')
        .substringBefore('?')
        .trim()

    companion object {
        // Engines with no generic implementation; routed to a native MangaRepository in the Factory.
        private val NATIVE_BACKED_ENGINES = setOf("mangafire")
        private const val CACHE_FILE = "datadriven-catalogue.json"
        private const val MAX_CATALOGUE_BYTES = 16L * 1024 * 1024
        // Debug-only default (R8 strips it from release, so the store build ships no URL).
        private const val DEBUG_DEFAULT_URL =
            "https://raw.githubusercontent.com/Nyora-Manga/nyora-data-driven/main/catalogue.json"
    }
}
