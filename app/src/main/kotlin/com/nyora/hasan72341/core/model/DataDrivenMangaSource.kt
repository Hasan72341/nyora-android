package com.nyora.hasan72341.core.model

import com.nyora.hasan72341.mihon.parsers.model.MangaSource

/**
 * A source that is DATA, not code: rendered at runtime by one of the bundled generic engines
 * (madara, mangareader, …) from a [com.nyora.hasan72341...] repo-supplied definition, instead of a
 * per-source parser compiled into the APK. This is the source kind that lets the catalogue be
 * fetched at runtime (Play-compliant) rather than baked in.
 *
 * It carries everything needed to construct its engine — [engineKey] selects the generic engine,
 * [domain] + [config] are the pure data it reads — so [core.parser.MangaRepository.Factory] can wire
 * a repository without any code loading. [name] is the persisted identity (prefixed so it never
 * collides with a native [com.nyora.hasan72341.mihon.parsers.model.MangaParserSource] name).
 */
data class DataDrivenMangaSource(
    val sourceId: String,
    val engineKey: String,
    val title: String,
    val lang: String,
    val nsfw: Boolean,
    val domain: String,
    val config: Map<String, Any?> = emptyMap(),
) : MangaSource {

    override val name: String get() = PREFIX + sourceId

    companion object {
        const val PREFIX = "DD_"

        // Persisted state (favourites, history, source order) stores a source by its String name, so
        // resolving "DD_<id>" back to the full source needs the fetched catalogue. This registry is
        // populated by the catalogue repository and consulted by MangaSource(name) — mirroring how
        // the native path resolves a name against the compiled MangaParserSource enum.
        private val registry = java.util.concurrent.ConcurrentHashMap<String, DataDrivenMangaSource>()

        /** True if a persisted source name refers to a data-driven source. */
        fun isDataDriven(name: String): Boolean = name.startsWith(PREFIX)

        /** Replace the known catalogue used for name resolution. */
        fun register(sources: List<DataDrivenMangaSource>) {
            registry.clear()
            sources.forEach { registry[it.name] = it }
        }

        /** Resolve a persisted "DD_<id>" name to its source, or null if not in the current catalogue. */
        fun resolve(name: String): DataDrivenMangaSource? = registry[name]
    }
}
