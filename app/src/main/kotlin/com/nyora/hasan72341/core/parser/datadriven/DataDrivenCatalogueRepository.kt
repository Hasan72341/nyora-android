package com.nyora.hasan72341.core.parser.datadriven

import android.content.Context
import app.nyora.data.engine.EngineRegistry
import com.nyora.hasan72341.core.model.DataDrivenMangaSource
import com.nyora.hasan72341.core.network.MangaHttpClient
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
import kotlinx.serialization.json.doubleOrNull
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
) {

    private val cacheFile = File(context.filesDir, CACHE_FILE)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Volatile
    private var cached: List<DataDrivenMangaSource>? = null

    /** The currently known data-driven sources (disk cache until a [refresh] lands). */
    val sources: List<DataDrivenMangaSource>
        get() = cached ?: loadFromDisk().also { cached = it }

    /** Fetch the latest catalogue, replacing the cache. Returns the parsed source count. */
    suspend fun refresh(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(CATALOGUE_URL).build()
            val body = okHttpClient.newCall(request).execute().use { resp ->
                require(resp.isSuccessful) { "catalogue fetch failed: HTTP ${resp.code}" }
                resp.body?.string().orEmpty()
            }
            val parsed = parse(body)
            runCatching { cacheFile.writeText(body) } // cache best-effort; a write failure isn't fatal
            publish(parsed)
            parsed.size
        }
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
        val dto = json.decodeFromString<CatalogueDto>(text)
        return dto.sources.asSequence()
            .filterNot { it.broken }
            .filter { EngineRegistry.supports(it.engine) }
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
                    domain = row.domain,
                    config = config,
                )
            }
            .toList()
    }

    @Serializable
    private data class CatalogueDto(val sources: List<SourceRowDto> = emptyList())

    @Serializable
    private data class SourceRowDto(
        val id: String,
        val name: String = "",
        val lang: String = "en",
        val nsfw: Boolean = false,
        val engine: String,
        val domain: String,
        val broken: Boolean = false,
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

    companion object {
        private const val CACHE_FILE = "datadriven-catalogue.json"
        const val CATALOGUE_URL =
            "https://raw.githubusercontent.com/Nyora-Manga/nyora-data-driven/main/catalogue.json"
    }
}
