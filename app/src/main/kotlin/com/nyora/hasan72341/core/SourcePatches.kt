package com.nyora.hasan72341.core

/**
 * Runtime patches over the pinned `nyora-parsers-redo` dependency, whose source domains are
 * baked into the compiled library and cannot be edited in source. Mirrors the iOS port + the
 * desktop (nyora-mac/linux/windows) shared `SourcePatches`.
 *
 * - [DOMAIN_OVERRIDES]: rebranded/relocated sources whose default domain is dead; value is the
 *   current live domain (curl-verified or live-behind-Cloudflare). Applied as the effective
 *   ConfigKey.Domain default when the user has not set their own override.
 * - [DEAD_SOURCES]: sources whose domain is dead with no working same-CMS successor; excluded
 *   from `allMangaSources` so they never appear in the catalogue.
 * Keyed by the upstream `MangaParserSource.name`.
 */
object SourcePatches {
    val DOMAIN_OVERRIDES: Map<String, String> = mapOf(
        // AsuraComic relocated to asurascans.com (its baked-in asuracomic.net access-denies); the
        // /browse -> a[href*=/comics/] structure still matches the AsuraScansParser.
        "ASURASCANS" to "asurascans.com",
        "ALTAYSCANS" to "witchscans.com",
        "ASTRASCANS" to "astracomic.com",
        "FLOWERMANGA" to "flowermangas.net",
        "MANGAGEZGINI" to "mangagezgini.online",
        "MANHUAUSS" to "manhua.us.com",
        "MHSCANS" to "mhscans.com",
        "ISEKAISCAN_EU" to "isekaiscan.top",
        "RUYAMANGA" to "ruyamanga2.com",
        "TRUYENTRANHFULL" to "truyentranhfull.vip",
        "MANGATILKISI" to "tilkiscans.com",
        "YAOIBAR" to "yaoibar.lol",
        "AINZSCANS" to "ainzscans.net",
        "DOCTRUYEN3Q" to "doctruyen3qhub.org",
        "YUGEN_MANGAS_ES" to "visualikigai.com",
        "LEITORDEMANGA" to "leitordemangas.com",
        "LILYUMFANSUB" to "lilyumfansub.pro",
    )

    /** Display-name overrides for sources that rebranded along with their domain move. */
    val TITLE_OVERRIDES: Map<String, String> = mapOf(
        "ASTRASCANS" to "Astra Comic",
        "MANGATILKISI" to "Tilki Scans",
        "ISEKAISCAN_EU" to "IsekaiScan",
        "YUGEN_MANGAS_ES" to "Visual Ikigai",
        "TOONILY_ME" to "ToonDex",
    )

    val DEAD_SOURCES: Set<String> = setOf(
        // Dead AsuraScans clones (asurascans.us 403 / .gg redirect); the live Asura is
        // ASURASCANS -> asurascans.com (see DOMAIN_OVERRIDES).
        "ASURASCANS_US",
        "ASURASCANSGG",
        "ATEMPORAL",
        "AYATOON",
        "BANANA_MANGA",
        "DREAMSCAN",
        "EDSCANLATION",
        "ELEVENSCANLATOR",
        "FACTMANGA",
        "FREEMANGA",
        "FREEMANGATOP",
        "GMANGA",
        "GOURMETSCANS",
        "GUNCEL_MANGA",
        "HIKARISCAN",
        "HOIFANSUB",
        "KABUSMANGA",
        "KALANGO",
        "KORELISCANS",
        "KUMASCANS",
        "LEGENDSCANLATIONS",
        "LILYUMFANSUB",
        "MAFIAMANGA",
        "MANGAGOJO",
        "MANGAJINX",
        "MANGAKINGS",
        "MANGAKISS",
        "MANGAMATE",
        "MANGANINJA",
        "MANGAOKUTR",
        "MANGAONELOVE",
        "MANGAONLINETEAM",
        "MANGAREADCO",
        "MANGAROSE",
        "MANGASECT",
        "MANGASSCANS",
        "MANGATX_GG",
        "MANGA_MANHUA",
        "MANHUAES",
        "MANHUAGA",
        "MANHUAGOLD",
        "MANHUASCAN",
        "MANHWAKU",
        "MANHWASCO",
        "MANJANOON",
        "MOONWITCHINLOVESCAN",
        "MURIMSCAN",
        "NEWTRUYEN",
        "NIRVANASCAN",
        "NORTEROSE",
        "NOVELMIC",
        "RAYSSCAN",
        "READER_EVILFLOWERS",
        "REAPERCOMICS",
        "RUAHAPCHANHDAY",
        "SECTSCANS",
        "SEINAGI",
        "SHOOTINGSTARSCANS",
        "SITEMANGA",
        "SSREADING",
        "SWEETSCAN",
        "TATAKAE_SCANS",
        "TAURUSMANGA",
        "TCBSCANSMANGA",
        "TECNOSCANS",
        "TIMENAIGHT",
        "TOONILY_ME",
        "TRADUCCIONESAMISTOSAS",
        "TYRANTSCANS",
        "WEBTOONTR",
        "WINTERSCAN",
        "ZANDYNOFANSUB",
        "ZENITHSCANS",
        "ZINCHANMANGA_NET",
        "ZIN_MANGA_COM"
    )
}
