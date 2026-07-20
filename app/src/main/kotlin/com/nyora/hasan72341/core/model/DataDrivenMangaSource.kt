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

        /** True if a persisted source name refers to a data-driven source. */
        fun isDataDriven(name: String): Boolean = name.startsWith(PREFIX)
    }
}
