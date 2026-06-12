package com.nyora.hasan72341.tachiyomi.source

/**
 * A factory for creating sources at runtime.
 */
interface SourceFactory {
    /**
     * Create a new copy of the sources
     * @return The created sources
     */
    fun createSources(): List<Source>
}
