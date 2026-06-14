<div align="center">

<img src="https://nyora.pages.dev/icon.png" width="120" alt="Nyora"/>

# Nyora — Android

### Read like the world can wait.

A fast, free, ad-free, open-source manga reader for Android — 1100+ sources, whole-page AI translation typeset over the original art, offline downloads, and a buttery reader, with your library synced across every device.

<p>
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android"/>
  <img src="https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose"/>
  <img src="https://img.shields.io/badge/Material_You-6750A4?style=for-the-badge&logo=materialdesign&logoColor=white" alt="Material You"/>
  <img src="https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white" alt="Gradle"/>
</p>

[![License: GPL v3](https://img.shields.io/github/license/Hasan72341/nyora-android?color=blue)](LICENSE)
[![Latest release](https://img.shields.io/github/v/release/Hasan72341/nyora-android?label=download&color=0ae448)](https://github.com/Hasan72341/nyora-android/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/Hasan72341/nyora-android/total?color=9d95ff)](https://github.com/Hasan72341/nyora-android/releases)
[![Stars](https://img.shields.io/github/stars/Hasan72341/nyora-android?style=social)](https://github.com/Hasan72341/nyora-android/stargazers)

[![Download APK](https://img.shields.io/badge/Download-APK-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/Hasan72341/nyora-android/releases/latest)
[![Website](https://img.shields.io/badge/Website-nyora.pages.dev-FF4655?style=for-the-badge&logo=githubpages&logoColor=white)](https://nyora.pages.dev)
[![Open Web App](https://img.shields.io/badge/Open-Web_App-5A0FC8?style=for-the-badge&logo=pwa&logoColor=white)](https://nyoraweb.pages.dev)

</div>

<p align="center">
  <img src="https://nyora.pages.dev/screenshots/phone-1.png" width="200"/>
  <img src="https://nyora.pages.dev/screenshots/phone-3.png" width="200"/>
  <img src="https://nyora.pages.dev/screenshots/phone-5.png" width="200"/>
</p>

---

## About

Nyora is a polished, privacy-first manga reader for Android — a fork of [Kotatsu](https://github.com/KotatsuApp/Kotatsu) rebuilt around five things readers actually care about: translating a whole page of any language back over the original art, downloading chapters for offline reading, pulling from 1100+ built-in sources plus Tachiyomi/Mihon/Keiyoushi extensions, syncing your entire library across every device for free, and staying 100% open-source with no ads, no tracking and no account required to read. Read on your phone at lunch and pick up exactly where you left off on your laptop at night.

## Highlights

| Pillar | What it does |
|---|---|
| Translate | Detects every bubble and caption, translates the whole page, and typesets it back over the original artwork — with an on-device offline ML fallback. |
| Download | Save chapters with a tap for offline reading; open and read local CBZ archives too. |
| Sources | 1100+ built-in sources for manga, manhwa & manhua, plus native Tachiyomi / Mihon / Keiyoushi extension support. |
| Sync | Free Google cloud sync of library, categories, history, bookmarks and exact reading progress across every platform. |
| Open Source | 100% free, ad-free, no tracking, no sign-up to read. Android app under GPLv3; rest of the ecosystem Apache-2.0. |

## Table of Contents

- [About](#about)
- [Highlights](#highlights)
- [Features](#features)
  - [Translate](#translate)
  - [Download & Offline](#download--offline)
  - [Sources & Discovery](#sources--discovery)
  - [Cloud Sync](#cloud-sync)
  - [Reader](#reader)
  - [Trackers](#trackers)
  - [Privacy & Open Source](#privacy--open-source)
  - [Themes & Personalisation](#themes--personalisation)
- [Capability Matrix](#capability-matrix)
- [Screenshots](#screenshots)
- [Installation](#installation)
- [Build from Source](#build-from-source)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Nyora on Every Platform](#nyora-on-every-platform)
- [Roadmap](#roadmap)
- [FAQ](#faq)
- [Contributing](#contributing)
- [Acknowledgements](#acknowledgements)
- [License](#license)

## Features

### Translate

Translation is the headline feature. Tap translate and Nyora processes the **entire page at once**: it detects every speech bubble and caption, translates the text, and **typesets it back over the original artwork** so the page reads naturally in your language — panels, sound effects and all. No copy-pasting into another app, no manual cropping, no waiting on a scanlation that may never come.

When you are offline, or simply want everything to stay on-device, an **on-device offline ML fallback** keeps translation working with no network connection. That makes Nyora genuinely useful for raw chapters in any language: read the original art with translated text laid in place, wherever you are. Targets such as **English and Hindi** are supported, alongside the broader set of languages the translation engine handles.

### Download & Offline

Save chapters with a single tap and read on the train, on a flight, or anywhere the signal drops. Downloads are **yours to keep**, queued and managed in-app so you can batch large series and walk away while they finish. Saved chapters read with the same reader, gestures and per-title settings as online titles — nothing changes when you go offline. Nyora also **opens local CBZ archives**, so a collection you already own comes along for the ride and reads exactly like everything else.

### Sources & Discovery

Browse, search and filter across **1100+ built-in sources** spanning **manga, manhwa and manhua**. When you want more, Nyora speaks **Tachiyomi / Mihon / Keiyoushi** extensions natively — install the extension package you already use and its catalogues plug straight in, with no conversion step. A new-chapter update feed keeps ongoing series fresh, recommendations tailored to your library help surface your next read, and a fast search box gets you to a specific title in seconds. Nyora hosts none of this content; it is a reader that talks to the sources you choose.

### Cloud Sync

Sign in with **Google** and your world follows you. Your **library, custom categories, reading history, bookmarks and exact reading progress** stay in lockstep across **Android, iOS, macOS, Windows, Linux and the Web**. Switch devices mid-chapter and you never lose your place. Sync is **opt-in and account-based** — it exists only to carry your own reading state between your own devices — and it is completely **free**, with no subscription and no premium tier gating it. Prefer to stay local? Never sign in and Nyora works fully offline.

### Reader

The reader supports both **standard and webtoon** modes with **LTR / RTL / vertical** orientations, smooth gestures, pinch zoom and **double-page** layout for spreads. Settings can be applied **per-title**, so a long-strip webtoon and a right-to-left manga each behave the way they should and remember it next time. **Dynamic colour correction** lets you tune brightness, contrast and colour live while reading — handy for low-contrast raws or reading in the dark. Volume-key paging and tap zones keep one-handed reading comfortable.

### Trackers

Keep your progress in sync with the services you already use. Nyora integrates with **AniList, MyAnimeList, Shikimori and Kitsu**, updating your lists as you read so your reading and your tracker stay consistent without manual edits. Trackers handle your public reading lists; Nyora's own cloud sync handles your full library state — the two work side by side.

### Privacy & Open Source

Nyora is **100% free and ad-free**, with **no tracking and no account required to read** — signing in is only for optional cloud sync. The app ships an **app-lock** (PIN or fingerprint) that gates the app behind your device biometrics, and an **incognito mode** for private reading sessions that are not written to history. Because the project is open-source, you can read every line and verify exactly what the app does. The Android app is licensed under **GPLv3**; the rest of the Nyora ecosystem is Apache-2.0.

### Themes & Personalisation

Choose **light, dark or system** themes, plus a true-black **AMOLED** mode that switches the UI to pure black to save power on OLED panels and ease night reading. The app follows **Material You** conventions for a clean, native Android feel, adapting accent colours to your wallpaper on supported versions. Your library stays organised with **favourites in custom categories**, history and bookmarks.

## Capability Matrix

A quick, honest snapshot of what the Android app does today.

| Capability | Android |
|---|---|
| Whole-page AI translation (typeset over art) | Yes |
| On-device offline translation fallback | Yes |
| Built-in sources | 1100+ |
| Tachiyomi / Mihon / Keiyoushi extensions | Native |
| Offline downloads | Yes |
| Local CBZ archive reading | Yes |
| Standard + webtoon reader | Yes |
| LTR / RTL / vertical, double-page | Yes |
| Per-title reader settings | Yes |
| Free cloud sync (Google) | Yes |
| Trackers (AniList / MAL / Shikimori / Kitsu) | Yes |
| App-lock (PIN / fingerprint) | Yes |
| Incognito mode | Yes |
| AMOLED / Material You theming | Yes |
| Ads / tracking | None |
| Account required to read | No |

## Screenshots

### Phone

| | | |
|:-:|:-:|:-:|
| ![Home](docs/screenshots/01-home.png) | ![Explore](docs/screenshots/02-explore.png) | ![Library](docs/screenshots/03-library.png) |
| ![History](docs/screenshots/04-history.png) | ![Details](docs/screenshots/05-details.png) | ![Reader](docs/screenshots/06-reader.png) |
| ![Translate](docs/screenshots/07-translate.png) | ![Search](docs/screenshots/08-search.png) | ![Settings](docs/screenshots/09-settings.png) |

### Tablet

| | |
|:-:|:-:|
| ![Tablet — Home](docs/screenshots/tablet/01-home.png) | ![Tablet — Explore](docs/screenshots/tablet/02-explore.png) |
| ![Tablet — Favourites](docs/screenshots/tablet/03-favorites.png) | ![Tablet — History](docs/screenshots/tablet/04-history.png) |

## Installation

The recommended way to install Nyora is the signed release APK.

**Requirements**

- Android **6.0 (Marshmallow)** or newer
- Works on phones and tablets

**Steps**

1. Open the **[Releases page](https://github.com/Hasan72341/nyora-android/releases/latest)** and download the latest `nyora-*.apk`.
2. Open the downloaded file. If this is your first sideloaded app, Android will prompt you to **allow installs from your browser (or file manager)** — grant the permission and continue.
3. Confirm the install and launch Nyora.

**Updating**

Newer releases are published on the same Releases page. Download and install the new APK over your existing install — your library, downloads and settings are preserved, and if you use cloud sync your library is also restored on a fresh install once you sign in with Google.

**Troubleshooting**

- *"App not installed" / blocked by Play Protect*: choose **Install anyway** when prompted. Nyora is sideloaded and is not distributed through the Play Store.
- *Install permission missing*: go to **Settings → Apps → Special access → Install unknown apps**, select the browser or file manager you used, and enable it, then retry.

## Build from Source

**Prerequisites**

- **Android Studio**
- **JDK 17**

**Build**

```bash
git clone https://github.com/Hasan72341/nyora-android.git
cd nyora-android
./gradlew assembleRelease   # or open in Android Studio and Run ▸ app
```

The release APK is produced under the app module's build outputs. You can also open the project in Android Studio and use **Run ▸ app** to build and deploy a debug build to a connected device or emulator.

## Tech Stack

<p>
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android"/>
  <img src="https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose"/>
  <img src="https://img.shields.io/badge/Material_You-6750A4?style=for-the-badge&logo=materialdesign&logoColor=white" alt="Material You"/>
  <img src="https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white" alt="Gradle"/>
</p>

- **Kotlin** — the app is written in Kotlin, targeting modern Android.
- **Android** — native app supporting Android 6.0+ on phones and tablets.
- **Jetpack Compose** — used for modern, declarative UI.
- **Material You** — themed, native-feeling design with light/dark/AMOLED.
- **Gradle** — build system; release builds via `./gradlew assembleRelease`.

## Architecture

Nyora for Android is a fork of [Kotatsu](https://github.com/KotatsuApp/Kotatsu), inheriting its mature reader engine and source architecture and layering Nyora's pillars on top.

- **Sources layer** — 1100+ built-in sources are accessed through a pluggable source/parser architecture, with native compatibility for **Tachiyomi / Mihon / Keiyoushi** extensions so existing extension ecosystems work without conversion.
- **Reader engine** — a tuned image reader handles standard and webtoon modes, multiple orientations, zoom, double-page layout and live colour correction, with per-title settings persisted locally.
- **Translation pipeline** — whole-page detection, translation and typesetting render translated text back over the original artwork, with an **on-device offline ML fallback** so the feature works without a network connection.
- **Offline & local files** — a download manager queues and stores chapters for offline reading, and the same reader opens local **CBZ** archives.
- **Cloud sync** — signing in with **Google** synchronises library, categories, history, bookmarks and reading progress across all Nyora platforms, so state is consistent on every device.
- **Privacy surfaces** — app-lock (PIN/fingerprint) and incognito mode are built in; no tracking is performed and no account is required for local-only reading.

## Nyora on Every Platform

| Platform | Repo | Get it |
|---|---|---|
| Android | **nyora-android** *(you are here)* | [APK](https://github.com/Hasan72341/nyora-android/releases/latest) |
| Windows | [nyora-windows](https://github.com/Hasan72341/nyora-windows) | [.exe (x64/ARM64)](https://github.com/Hasan72341/nyora-windows/releases/latest) |
| macOS | [nyora-mac](https://github.com/Hasan72341/nyora-mac) | [.dmg / `brew`](https://github.com/Hasan72341/nyora-mac/releases/latest) |
| Linux | [nyora-linux](https://github.com/Hasan72341/nyora-linux) | [deb · rpm · curl](https://github.com/Hasan72341/nyora-linux/releases/latest) |
| iOS / iPadOS | [nyora-ios](https://github.com/Hasan72341/nyora-ios) | [sideload IPA](https://github.com/Hasan72341/nyora-ios/releases/latest) |
| Web | — | [nyoraweb.pages.dev](https://nyoraweb.pages.dev) |

All platforms share one synced library, categories, history, bookmarks and exact reading progress through the same free Google sign-in.

## Roadmap

Honest, already-stated directions for the wider Nyora family — no dates, no promises:

- **iOS** — a signed TestFlight build is planned and will follow the current sideloaded IPA.
- **Source parity** — continued expansion and parity work across newer source ports.

## FAQ

**Is Nyora free? Are there ads?**
Yes, it is 100% free and completely ad-free. There is no premium tier, no subscription and no tracking.

**Do I need an account?**
No account is required to read. Signing in with Google is only for optional cloud sync of your library and progress.

**Where does the content come from?**
Nyora hosts no manga. It accesses 1100+ built-in sources and is compatible with Tachiyomi / Mihon / Keiyoushi extensions — it is a reader, not a host, and is not affiliated with any of the sources it can access.

**Are my existing extensions compatible?**
Yes. Nyora supports Tachiyomi / Mihon / Keiyoushi extensions natively, so the extension packages you already use plug straight in without conversion.

**Does cloud sync need an account, and is it private?**
Sync is opt-in and tied to your own Google sign-in. It carries only your library, categories, history, bookmarks and reading progress between your own devices. Reading itself never requires an account — you only sign in if you want sync.

**Can I read offline?**
Yes. Download chapters in-app for offline reading, and open local CBZ archives you already own. An on-device offline ML translation fallback also works without a connection.

**Which Android versions are supported?**
Android 6.0 and newer, on both phones and tablets.

**How do I update the APK?**
Download the newest APK from the Releases page and install it over your existing version. Your library, downloads and settings are preserved, and cloud sync restores your library after a fresh install once you sign back in.

**Which other platforms share my library?**
Android, Windows, macOS, Linux, iOS/iPadOS and the Web — all sync the same library and progress. See [Nyora on Every Platform](#nyora-on-every-platform).

## Contributing

[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-0ae448?style=for-the-badge)](https://github.com/Hasan72341/nyora-android/pulls)

Contributions are welcome — bug reports, new sources, translations and UI polish especially. The best place to start is the project's [Issues](https://github.com/Hasan72341/nyora-android/issues) and [Pull Requests](https://github.com/Hasan72341/nyora-android/pulls). Clone the repo, follow the [Build from Source](#build-from-source) steps, and open a PR with a clear description of your change.

If Nyora makes your reading better, the simplest way to help is to [star the repo](https://github.com/Hasan72341/nyora-android/stargazers) — it genuinely helps the project grow and reach more readers.

## Acknowledgements

Nyora for Android is built on the excellent [Kotatsu](https://github.com/KotatsuApp/Kotatsu) reader, and stands on the work of its maintainers and contributors. Thanks also to the Tachiyomi / Mihon / Keiyoushi extension communities, and to everyone who reports issues and contributes improvements.

Developed and maintained by **Md Hasan Raza** — [GitHub](https://github.com/Hasan72341) · [Instagram](https://instagram.com/md_hasan_raza____) · [LinkedIn](https://www.linkedin.com/in/md-hasan-raza) · hasanraza96@outlook.com

## License

Licensed under the **GNU General Public License v3.0** — see [`LICENSE`](LICENSE). Nyora for Android is a fork of [Kotatsu](https://github.com/KotatsuApp/Kotatsu).

> Nyora is not affiliated with any of the manga sources it can access.