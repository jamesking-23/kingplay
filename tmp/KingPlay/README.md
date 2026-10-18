# KingPlay

Premium local-first music and video player for Android (Kotlin, Jetpack Compose, Media3).
Phase 1 (MVP) is built step by step; this drop is **step 2b: database, library scanner, repositories** (on top of 2a: skeleton + design system).

## Setup
1. Install Android Studio (latest stable) with JDK 17 and Android SDK 35.
2. Open this folder. If there is no `gradlew`, run once: `gradle wrapper --gradle-version 8.10.2`.
3. Run the `app` configuration. The home screen is a live design-system gallery.
4. Checks: `./gradlew test detekt ktlintCheck`; on a device or emulator also `./gradlew :core:database:connectedAndroidTest`.
   CI should add `-PwarningsAsErrors=true`.

## Structure
- `build-logic/` convention plugins: `kingplay.android.application|library|compose|hilt`, `kingplay.jvm.library`.
- `gradle/libs.versions.toml` is the single version catalog.
- `:app` entry point and DI wiring.
- `:core:model` pure Kotlin models, navigation contract (`AppNavigator`), future-phase service interfaces.
- `:core:common` dispatchers, feature flags, entitlements (pro unlocked stub), consent-gated analytics.
- `:core:designsystem` themes (Light, Dark, AMOLED, Material You, 4 accents), type, spacing, motion, components.
- `:core:ui` MVI base ViewModel, `AsyncContent` (loading/empty/error), adaptive layout helper.
- `:core:database` Room schema v1 (tracks, videos, playlists, queue, EQ presets, device profiles, FTS4 search), DAOs, mappers.
- `:data:library` MediaStore scanner, Room-backed repositories, WorkManager scan worker, M3U import/export.
  Repository interfaces live in `:core:model` (`com.kingplay.core.model.repository`) so features never see Room.

## Library data layer (step 2b)
- **Ids**: tracks and videos use the MediaStore id. Artist and album ids are stable hashes of names (`StableIds`), so they
  survive rescans. Without an album-artist tag, the folder groups an album (compilation folders stay together).
- **Scan**: fingerprints (id, modified, size, folder) for the whole library, diffed against Room; full rows are loaded only
  for new or changed ids, in chunks. A second pass (`DetailsEnricher`) reads sample rate and bitrate and is resumable.
  Scans never delete when the platform returns nothing at all (revoked permission, unmounted card).
- **Triggers**: app start, a debounced MediaStore observer, a daily periodic scan, and `LibraryScanner.requestScan()`.
- **User data** (favorites, play counts, resume positions) lives in separate tables, so rescans cannot erase it.
- **Search**: FTS4 prefix search (`FtsQuery.build`) over title, artist, album and genre, plus albums and artists.
- **Adding a schema change**: bump `KingPlayDatabase.VERSION`, add a `Migration` in `DatabaseModule`, commit the new file in
  `core/database/schemas/`. Never use destructive fallback: playlists live in this database.
- Temporary `LibraryStatusScreen` in `:app` shows permission, scan progress, counts and newest tracks. Replaced in step 2d.

## Playback (step 2c)

- `:core:player` owns one ExoPlayer inside a Media3 `MediaLibraryService` (`PlaybackService`), so the notification,
  lock-screen controls and headset buttons work. The player is service-scoped, never a singleton.
- The UI talks to it through `PlaybackController` (in `:core:model`). It sends id-only items (`a:<id>`, `v:<id>`);
  the service resolves them to URIs and metadata from Room.
- The queue, position, shuffle and repeat are saved to Room and restored (paused) on the next start. Files that no
  longer exist are dropped on restore.
- A play counts after half the track or 30 s, whichever comes first, with a 5 s floor.
- On Android 13+ the notification needs the POST_NOTIFICATIONS permission; the dev screen asks for it.
- Dev screen: tap a recent track to play it, then use Previous / Play-Pause / Next in the bar.

Not built yet: crossfade and EQ (2e), artwork (2d), Android Auto browse tree (2h).

## Known limits (by design, tracked for later steps)
- Tag editing and embedded ReplayGain reading land with step 2e (columns already exist, currently null).
- External subtitle discovery is part of the video step (2g); `VideoItem.subtitleTracks` is empty for now.
- Playlists refer to MediaStore ids. If Android rebuilds its media index, ids change; the backup step (2h) will relink by path.

## Rules
- Feature modules never depend on each other; they use `AppNavigator`.
- Room is the single source of truth (from step 2b).
- Analytics only through `AnalyticsTracker`, gated by `ConsentGate`.

## Add a feature module
1. Create `feature/<name>/build.gradle.kts` with `kingplay.android.library`, `kingplay.android.compose`, `kingplay.android.hilt`.
2. Depend only on `:core:*` modules. Include it in `settings.gradle.kts` and add it to `:app`.
3. Expose a `NavGraphBuilder` extension; navigate elsewhere through `AppNavigator`.

(How to add an audio effect is documented in step 2e, when the processor chain lands.)
