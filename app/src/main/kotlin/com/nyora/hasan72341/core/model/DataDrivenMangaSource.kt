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
    /**
     * Mihon/Keiyoushi source ids that read the SAME site as this source.
     *
     * Mihon identifies a source only by a 64-bit id derived from the extension's own name/lang, so
     * nothing in a Mihon-shaped reference can be translated to a Nyora id by string surgery. The
     * catalogue therefore ships the correspondence, computed offline by joining the extension repo
     * indexes against this catalogue on the site each side reads (see nyora-data-driven
     * tools/build-mihon-bridge.py). Empty for the majority of sources, which have no Mihon twin.
     */
    val mihonIds: List<Long> = emptyList(),
    /**
     * Catalogue liveness flag. A broken source is kept RESOLVABLE (so an entry that references it
     * still shows the right source instead of reading as corrupted) but is left out of the
     * browsable list — identity and browsability are separate concerns, and this flag is a
     * point-in-time observation that a domain patch or the site itself can reverse.
     */
    val broken: Boolean = false,
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

        // Mihon source id -> the catalogue source reading the same site. Lets a library imported or
        // synced from Mihon resolve to a source this app can actually open, instead of Unknown.
        @Volatile
        private var byMihonId: Map<Long, DataDrivenMangaSource> = emptyMap()

        fun isDataDriven(name: String): Boolean = name.startsWith(PREFIX)

        fun register(sources: List<DataDrivenMangaSource>) {
            registry = sources.associateByTo(HashMap(sources.size)) { it.name }
            // Last one wins on a case collision; acceptable — these ids are practically unique.
            byLowerId = sources.associateByTo(HashMap(sources.size)) { it.sourceId.lowercase() }
            byMihonId = buildMap {
                for (source in sources) {
                    // A Mihon id maps to exactly one site, so a collision here means the catalogue
                    // has two rows claiming it; keep the first so the result stays deterministic
                    // regardless of catalogue ordering.
                    for (id in source.mihonIds) putIfAbsent(id, source)
                }
            }
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

        /**
         * Resolve a Mihon/Keiyoushi source id to the catalogue source reading the same site, or
         * null when the catalogue carries no equivalent.
         *
         * This is a FALLBACK, not a redirect: when the user actually has the Mihon extension
         * installed it is resolved by the extension manager and never reaches here. It only fires
         * for a reference the app would otherwise have to render as an unknown, broken source —
         * a library imported from Mihon, synced from another client, or left behind by an
         * extension that has since been uninstalled.
         */
        fun resolveMihonId(mihonId: Long): DataDrivenMangaSource? = byMihonId[mihonId]

        /** As [resolveMihonId], for a persisted `MIHON_<id>` source name. */
        fun resolveMihonName(name: String): DataDrivenMangaSource? {
            if (!name.startsWith(MIHON_PREFIX)) return null
            val id = name.removePrefix(MIHON_PREFIX).toLongOrNull() ?: return null
            return resolveMihonId(id)
        }

        const val MIHON_PREFIX = "MIHON_"
    }
}
