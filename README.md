# KingPlay

Premium local-first music and video player for Android (Kotlin, Jetpack Compose, Media3).
Phase 1 (MVP) is built step by step; this drop is **step 2d: Library and Now Playing screens** (on top of 2a skeleton and design system, 2b database and scanner, 2c playback service).

## Setup
1. Install Android Studio (latest stable) with JDK 17 and Android SDK 35.
2. Open this folder. If there is no `gradlew`, run once: `gradle wrapper --gradle-version 8.10.2`.
3. Run the `app` configuration. First launch asks for media access, then shows your library. The settings icon opens a
   temporary settings screen (the design-system gallery with theme controls).
4. Checks: `./gradlew test detekt ktlintCheck`; on a device or emulator also `./gradlew :core:database:connectedAndroidTest`.
   CI should add `-PwarningsAsErrors=true`.

## Structure
- `build-logic/` convention plugins: `kingplay.android.application|library|compose|hilt`, `kingplay.jvm.library`.
- `gradle/libs.versions.toml` is the single version catalog.
- `:app` entry point and DI wiring.
- `:core:model` pure Kotlin models, navigation contract (`AppNavigator`), future-phase service interfaces.
- `:core:common` dispatchers, feature flags, entitlements (pro unlocked stub), consent-gated analytics.
- `:core:designsystem` themes (Light, Dark, AMOLED, Material You, 4 accents), type, spacing, motion, components.
- `:core:ui` MVI base ViewModel, `AsyncContent` (loading/empty/error), adaptive layout helper, cover-art loading
  (`ArtworkImage`), the shared list row (`MediaRow`) and formatters.
- `:feature:library` Library home (Songs, Albums, Artists, Genres, Folders, Playlists tabs) and the detail screens.
- `:feature:nowplaying` Now Playing screen, queue sheet and the mini player.
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

## Playback (step 2c)

- `:core:player` owns one ExoPlayer inside a Media3 `MediaLibraryService` (`PlaybackService`), so the notification,
  lock-screen controls and headset buttons work. The player is service-scoped, never a singleton.
- The UI talks to it through `PlaybackController` (in `:core:model`). It sends id-only items (`a:<id>`, `v:<id>`);
  the service resolves them to URIs and metadata from Room.
- The queue, position, shuffle and repeat are saved to Room and restored (paused) on the next start. Files that no
  longer exist are dropped on restore.
- A play counts after half the track or 30 s, whichever comes first, with a 5 s floor.
- On Android 13+ the notification needs the POST_NOTIFICATIONS permission; the permission screen asks for it together
  with media access.

Not built yet: crossfade and EQ (2e), Android Auto browse tree (2h).

## Screens (step 2d)

- **Navigation**: `:app` owns the nav graph (`KingPlayRoot`) and implements `AppNavigator`. Features never depend on each other.
  The mini player sits above the system bar on every screen except Now Playing.
- **Songs tab** is paged (Room `PagingSource`). Tapping a song builds the queue from the full sorted list through
  `LibraryRepository.songIds`, windowed to 2,000 items around the tapped song so a 50,000-song library does not flood
  the player (`queueWindow`). Shuffle all starts at a random song and turns on the player's shuffle mode.
- **Detail screens** (album, artist, genre, folder, playlist) share one `DetailViewModel` and `DetailScreen`; each only says
  where its title and songs come from. Playlists can be created, deleted, and have tracks removed.
- **Cover art** (`ArtworkImage`): Android 10+ asks the media store for the thumbnail; older versions and files without one
  read the embedded picture. One decoded bitmap is shared by every track of an album (cache key `album:<id>`).
- **Accessibility**: every row is a single TalkBack stop that reads title and subtitle, the overflow menu is separate, the
  seek bar exposes "1:23 of 3:45", queue reordering uses menu items (Move up / Move down) instead of drag only.
- Queue items are resolved by id in the service, so Now Playing shows titles by looking them up, not from the player.

## Audio engine (step 2e)

- `core/player/.../dsp` is pure Kotlin (unit tested): 10-band peaking EQ (31 Hz to 16 kHz), 100 Hz bass shelf, preamp,
  loudness, ReplayGain, automatic headroom and a soft limiter.
- `engine/KingPlayAudioProcessor` runs that chain inside ExoPlayer's audio sink (16-bit and float PCM; other formats play
  untouched). `AudioEngineBinder` feeds it the settings, reads ReplayGain from the playing song's tags, applies tempo and
  pitch, and runs the fade.
- `DefaultAudioSettingsRepository` keeps the live settings. When the output (speaker, wired, Bluetooth, USB) has a saved
  profile it is applied automatically; "Remember for this output" in the equalizer saves one.
- Equalizer screen: Now Playing top bar, equalizer icon.

## Known limits (by design, tracked for later steps)
- Tag editing is not built yet. ReplayGain is read from the file while it plays; the library columns for it stay null.
- External subtitle discovery is part of the video step (2g); `VideoItem.subtitleTracks` is empty for now.
- Playlists refer to MediaStore ids. If Android rebuilds its media index, ids change; the backup step (2h) will relink by path.

- No search screen yet (`SearchRepository` exists); no video library or player until 2g.
- Playlist rename and drag-to-reorder are not built yet; the queue can be reordered with Move up / Move down.
- Settings is a placeholder (the design gallery with theme controls) until step 2h; theme choices are not persisted yet.
- Genres and folders list songs only; an album grid view and per-tab sort options (other than Songs) come with polish in 2i.
- Fade is a fade out and in per song, not an overlapping crossfade (that needs a second player).
- The output device is guessed (USB, wired, Bluetooth, speaker) because Android before 13 does not say which is active.
- The EQ uses 16-bit or float output; bit-perfect / USB exclusive output is not part of this step.

## Rules
- Feature modules never depend on each other; they use `AppNavigator`.
- Room is the single source of truth (from step 2b).
- Analytics only through `AnalyticsTracker`, gated by `ConsentGate`.

## Add a feature module
1. Create `feature/<name>/build.gradle.kts` with `kingplay.android.library`, `kingplay.android.compose`, `kingplay.android.hilt`.
2. Depend only on `:core:*` modules. Include it in `settings.gradle.kts` and add it to `:app`.
3. Expose a `NavGraphBuilder` extension; navigate elsewhere through `AppNavigator`.

(How to add an audio effect is documented in step 2e, when the processor chain lands.)
