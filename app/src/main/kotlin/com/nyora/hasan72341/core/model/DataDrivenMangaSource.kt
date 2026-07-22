package com.nyora.hasan72341.core.model

import com.nyora.hasan72341.mihon.parsers.model.MangaSource

/**
 * A source defined by data, not code: [engineKey] picks a bundled generic engine that renders it from
 * [domain] + [config]. [name] is the persisted identity, prefixed so it can't collide with a native one.
 */
data class DataDrivenMangaSource(
    val sourceId: String,
    val engineKey: String,
    val title: String,
    val lang: String,
    val nsfw: Boolean,
    val domain: String,
    // Catalogue content type (MANGA/MANHWA/HENTAI/COMICS/…); null when untagged.
    val contentType: String? = null,
    val config: Map<String, Any?> = emptyMap(),
) : MangaSource {

    override val name: String get() = PREFIX + sourceId

    companion object {
        const val PREFIX = "DD_"

        // Maps "DD_<id>" -> source so persisted names resolve back to the fetched catalogue.
        // Swapped atomically so a concurrent resolve() never sees a half-populated map.
        @Volatile
        private var registry: Map<String, DataDrivenMangaSource> = emptyMap()

        // Reverse index by lower-cased sourceId, for resolving legacy/cross-client NATIVE source ids
        // (e.g. "parser:MANGADEX", "JS_MANGAFIRE_JA", bare "SUSHISCANFR") that other Nyora clients
        // sync — the catalogue id often differs only in case (mangadex / SUSHISCANFR / sushiscanfr).
        @Volatile
        private var byLowerId: Map<String, DataDrivenMangaSource> = emptyMap()

        fun isDataDriven(name: String): Boolean = name.startsWith(PREFIX)

        fun register(sources: List<DataDrivenMangaSource>) {
            registry = sources.associateByTo(HashMap(sources.size)) { it.name }
            // Last one wins on a case collision; acceptable — these ids are practically unique.
            byLowerId = sources.associateByTo(HashMap(sources.size)) { it.sourceId.lowercase() }
        }

        fun resolve(name: String): DataDrivenMangaSource? = registry[name]

        /**
         * Resolve a NATIVE/cross-client source id to its data-driven equivalent when the catalogue
         * has one. Strips the `parser:` / `JS_` / `script:` prefix and matches case-insensitively.
         * Returns null when no catalogue source corresponds (e.g. a source never ported to DD).
         */
        fun resolveNativeId(nativeName: String): DataDrivenMangaSource? {
            val bare = nativeName
                .substringAfter("parser:")
                .removePrefix("JS_")
                .substringAfter("script:")
                .trim()
            if (bare.isEmpty()) return null
            return byLowerId[bare.lowercase()]
        }
    }
}
