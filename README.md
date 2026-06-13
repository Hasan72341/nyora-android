# Nyora (にょら) — Android

Nyora is a free, open-source, ad-free manga reader for Android, built on the Kotatsu
engine with a premium reading experience and AI-powered page translation. Read
manga, manhwa and manhua from a huge catalogue of online sources, organise your
library, and keep everything in sync across all your devices.

> **Download:** grab the latest signed APK from the [Releases page](https://github.com/Hasan72341/nyora-android/releases/latest). Supports **Android 6.0+** (phones and tablets).

## Features

### Sources & discovery
- **1100+ built-in sources** — browse, search and filter a huge catalogue of online manga/manhwa/manhua sources out of the box, with multi-language support.
- **Extension support** — install third-party **Tachiyomi / Mihon / Keiyoushi** extensions for even more sources.
- **Explore & recommendations** — discover new titles by genre/tag, and get recommendations filtered to your taste based on your library.

### Reading
- **Standard & Webtoon reader** — left-to-right, right-to-left, and vertical webtoon modes, with full gesture support, zoom, double-page spreads, and per-title overrides.
- **AI page translation** — translate a whole manga page at once: text is detected, translated, and typeset back over the original art in context. Uses high-context LLM translation online, with an **on-device ML Kit offline fallback** when you have no connection.
- **Dynamic colour correction** — tune brightness, contrast and colour filters live while reading for the perfect look on any screen (great for low-quality scans or OLED).
- **Smooth navigation** — pages are cached and preloaded, with seamless transitions between chapters; keep-screen-on while reading.

### Library & organisation
- **Favourites in custom categories** — organise saved titles into your own user-defined categories.
- **Reading history & bookmarks** — pick up exactly where you left off; bookmark pages.
- **Incognito mode** — read without recording anything in history.
- **Offline downloads & CBZ** — download chapters to read offline anywhere, and open third-party **CBZ** archives from local storage.

### Updates & tracking
- **Updates feed & notifications** — a live feed of new chapters for the titles you follow, with new-chapter notifications.
- **Tracker integration** — sync your reading progress with **AniList, MyAnimeList, Shikimori and Kitsu**.

### Sync, privacy & theming
- **Cloud sync** — sign in with Google and your library, favourites, categories, reading history and progress sync automatically across all your devices (Supabase backend).
- **App-lock** — protect the app with a password or fingerprint/biometric lock.
- **No ads, no tracking** — completely free and open source.
- **Theming** — light / dark / system themes, accent colours, and a true-black AMOLED mode.

## Build from source

Requires **Android Studio** (Ladybug+) and **JDK 17**.

```bash
git clone https://github.com/Hasan72341/nyora-android.git
cd nyora-android
./gradlew assembleRelease        # signed release APK (needs keystore + signing props)
# or open the project in Android Studio and Run ▸ app
```

Releases are built and published automatically by GitHub Actions: pushing a `v<x.y>`
tag (matching the `versionName` in `app/build.gradle`) builds the signed APK on a
hosted runner and publishes it to a GitHub Release. Supabase/Google config is
injected from repository secrets at build time, with baked production defaults as a
fallback.

## Author & license

Developed and maintained by **Md Hasan Raza** — [GitHub](https://github.com/Hasan72341) · [Instagram](https://instagram.com/md_hasan_raza____) · [LinkedIn](https://www.linkedin.com/in/md-hasan-raza) · hasanraza96@outlook.com

Licensed under the **GNU General Public License v3.0** (see [`LICENSE`](LICENSE)). Nyora is a fork of [Kotatsu](https://github.com/KotatsuApp/Kotatsu) and is not affiliated with any of the manga sources it can access.
