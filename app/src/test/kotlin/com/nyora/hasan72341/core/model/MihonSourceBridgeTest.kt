package com.nyora.hasan72341.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

/**
 * The Mihon -> catalogue source bridge.
 *
 * Mihon persists a library entry's source as a bare 64-bit id, so a backup or a sync from a Mihon
 * client arrives as `MIHON_<id>` with nothing else to go on. Without the bridge every such entry
 * resolves to [UnknownMangaSource] and the whole imported library reads as broken; these tests pin
 * the fallback that keeps it usable.
 */
class MihonSourceBridgeTest {

    private val mangadex = DataDrivenMangaSource(
        sourceId = "mangadex",
        engineKey = "mangadex",
        title = "MangaDex",
        lang = "en",
        nsfw = false,
        domain = "mangadex.org",
        // MangaDex ships one Keiyoushi extension per language, all reading the same site.
        mihonIds = listOf(2499283573021220255L, 1L),
    )

    private val weebcentral = DataDrivenMangaSource(
        sourceId = "WEEBCENTRAL",
        engineKey = "weebcentral",
        title = "Weeb Central",
        lang = "en",
        nsfw = false,
        domain = "weebcentral.com",
        mihonIds = listOf(1234567890L),
    )

    /**
     * A source whose site is currently down. It must stay resolvable: the flag is a liveness
     * observation, and an entry that renders as "Unknown" is indistinguishable from corruption,
     * whereas one bound to a known-down source starts working again when the site does.
     */
    private val deadSite = DataDrivenMangaSource(
        sourceId = "DEADSCAN",
        engineKey = "madara",
        title = "DeadScan",
        lang = "en",
        nsfw = false,
        domain = "deadscan.example",
        mihonIds = listOf(555L),
        broken = true,
    )

    private val noTwin = DataDrivenMangaSource(
        sourceId = "SOMESCAN",
        engineKey = "madara",
        title = "SomeScan",
        lang = "en",
        nsfw = false,
        domain = "somescan.example",
    )

    @Before
    fun setUp() {
        DataDrivenMangaSource.register(listOf(mangadex, weebcentral, deadSite, noTwin))
    }

    @Test
    fun `resolves a mihon id to the catalogue source reading the same site`() {
        assertSame(mangadex, DataDrivenMangaSource.resolveMihonId(2499283573021220255L))
        assertSame(weebcentral, DataDrivenMangaSource.resolveMihonId(1234567890L))
    }

    @Test
    fun `every mihon id of a multi-language extension maps to the one source`() {
        assertSame(mangadex, DataDrivenMangaSource.resolveMihonId(1L))
    }

    @Test
    fun `unknown mihon id does not resolve`() {
        assertNull(DataDrivenMangaSource.resolveMihonId(999_999L))
    }

    @Test
    fun `persisted MIHON_ name resolves through the source factory`() {
        // This is the path a restored library actually takes: a stored source name, not an id.
        assertSame(mangadex, MangaSource("MIHON_2499283573021220255"))
    }

    @Test
    fun `MIHON_ name with no catalogue twin stays unknown rather than mis-linking`() {
        assertSame(UnknownMangaSource, MangaSource("MIHON_424242"))
    }

    @Test
    fun `malformed MIHON_ name is not mistaken for a parser source`() {
        assertSame(UnknownMangaSource, MangaSource("MIHON_not_a_number"))
    }

    @Test
    fun `bridge does not shadow native or data-driven resolution`() {
        assertSame(noTwin, MangaSource("DD_SOMESCAN"))
        assertEquals(LocalMangaSource, MangaSource("LOCAL"))
    }

    @Test
    fun `a source whose site is down still resolves`() {
        assertSame(deadSite, DataDrivenMangaSource.resolveMihonId(555L))
        assertSame(deadSite, MangaSource("MIHON_555"))
        assertSame(deadSite, MangaSource("DD_DEADSCAN"))
    }

    @Test
    fun `re-registering the catalogue replaces the bridge instead of accumulating`() {
        DataDrivenMangaSource.register(listOf(weebcentral))
        assertNull(DataDrivenMangaSource.resolveMihonId(2499283573021220255L))
        assertSame(weebcentral, DataDrivenMangaSource.resolveMihonId(1234567890L))
    }
}
