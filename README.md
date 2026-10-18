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
- `:feature:settings` Settings screen: theme, library scan, privacy, backup and restore.
- `:data:settings` DataStore-backed app settings and analytics consent (off by default).
- `:feature:video` Video player screen: gestures, controls, speed, audio and subtitle tracks, picture-in-picture.
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

Android Auto and the widget arrived in step 2i (below).

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

## Lyrics, visualizer, sleep timer (step 2f)

- Now Playing has three new buttons next to the heart: lyrics, visualizer and sleep timer. Lyrics and visualizer replace the
  cover art in the big square; tap the button again to go back.
- Lyrics come from the song's own tags: ID3 USLT and SYLT, Vorbis/FLAC `LYRICS`, MP4. Timed lyrics (LRC text or SYLT)
  highlight and follow the music; tap a line to jump there. Plain lyrics scroll freely.
- The visualizer is 32 bars from a 1024-point FFT run inside the audio processor, and only while it is on screen.
  With low-power visuals on it updates about 15 times a second.
- Sleep timer: 5 to 90 minutes, end of this song, or after 3 songs. It fades out over the last 30 seconds, can be
  extended by 10 minutes, and stops exactly at the end of the song for "end of this song".

## Video player (step 2g)
- Library has a Videos tab (thumbnail, quality, resume point). Tapping a video opens the full-screen player.
- Videos play in the same playback service as music. The queue is the other videos in the same folder, in natural order
  ("Episode 2" before "Episode 10"), so next and previous move between episodes. Starting a video replaces the music queue.
- Resume: the position is saved every 5 seconds and when you leave; it is ignored near the very start and end.
- Gestures: tap shows or hides controls, double tap left or right skips 10 seconds, double tap in the middle pauses,
  hold for 2x, swipe sideways to scrub, swipe up or down on the left for brightness and on the right for volume.
  Every gesture also has a button. The lock button ignores all touches except the unlock button.
- Speed 0.5x to 3x, picture size (fit, zoom, stretch), audio track and subtitle track pickers, subtitle size, rotate.
- Subtitles: embedded tracks are listed. For a separate file use "Load subtitle file" (.srt, .vtt, .ass, .ttml).
- Picture-in-picture: button, and automatic when you leave during playback on Android 12+.

## Settings, backup, lyrics import (step 2h)
- Settings is a real screen now and the theme (mode, accent, wallpaper colours, low-power visuals) is saved and applied
  to the whole app. Other screens: scan for new music, scan everything again, folders left out of the library
  (with "include again"), analytics consent (off; no analytics service is connected in this version), about.
- Backup: one `.json` file with playlists, favourites, saved EQ presets, left-out folders, imported lyrics and settings.
  Songs are stored as file paths and matched again by path on restore (same loose matching as M3U import), so a backup
  works after Android rebuilds its media index or on a new phone. Restore adds to what you have: playlists with an
  existing name are skipped, existing EQ presets and lyrics are kept. Music files and video entries are not included.
- Lyrics import: when a song has no lyrics, Now Playing offers "Import .lrc file". Imported lyrics are stored by
  artist and title and win over tags; "Remove imported lyrics" brings the tags back.
- The database is now version 2 (new `imported_lyrics` table) with a real migration. Nothing is wiped.

## Android Auto, resume, cover art, widget (step 2i)
- **Android Auto / Assistant:** `PlaybackService` is a `MediaLibraryService`. Browse tree (`core/player/browse/BrowseTree`):
  four root tabs (Recent, Playlists, Albums, Artists); Favorites, Most played and Recently added sit at the top of Playlists.
  Lists are capped (100 songs, 200 folders) and paged. Voice "play X" is resolved through library search.
- **Resume:** the media/play button restores the saved queue (`onPlaybackResumption`).
- **Cover art:** lock screen and notification now show the cover read from the file (`ArtworkBitmapLoader`).
- **Widget (`:feature:widget`):** 4x2 Glance widget with cover, title, artist, previous / play-pause / next.
  Buttons send media keys to the service; the body opens the app. It is fed through the optional `NowPlayingPublisher`
  binding, so `core:player` does not depend on it.
- Limits: Auto browse lists show no cover art (the car cannot read our file URIs); music only; the widget shows audio, not video.
  Not compiled or run on a device, Desktop Head Unit or launcher: the Media3 and Glance calls were checked only against stand-in declarations.

## Polish and accessibility (step 2j)
- Song rows and the mini player are announced as buttons; the playing song is announced as "selected".
- Decorative motion (visualizer, lyric glow, shimmer) is reduced by the in-app low-power switch **or** the system "remove animations" setting.
- `.github/workflows/ci.yml` runs detekt, lint, unit tests and a debug build. The repo ships no Gradle wrapper yet, so CI installs Gradle 8.10.2; commit a wrapper (`gradle wrapper --gradle-version 8.10.2`) and switch the steps to `./gradlew`.
- Security note: `PlaybackService` is exported (Android Auto and Assistant need that) and any app can browse the library tree. The data is local only, but restricting callers to a package allow-list is a sensible hardening step before release.

## PC version (step 3a)
Compose Multiplatform Desktop, Windows first. Module `:desktop`; it reuses `:core:model` (tracks, `PlaybackController`, JSON codec) and does not touch the Android modules.
- **Playback:** `VlcjAudioEngine` drives libVLC, so **64-bit VLC must be installed** (videolan.org). If it is missing the app says so instead of crashing. The queue rules (`PlayQueue`) and `DesktopPlaybackController` are plain Kotlin behind an `AudioEngine` interface and are unit-tested with a fake engine.
- **Library:** pick folders; `TrackScanner` walks them (never following links), reads tags with jaudiotagger and keeps unchanged files without re-reading. Ids come from the file path, so they survive rescans. Saved in `%APPDATA%\KingPlay\library.json` (written atomically; a damaged file just triggers a rescan).
- **UI:** Songs (with search), Albums, Artists, Folders; now-playing bar with seek, volume, shuffle, repeat; Windows media keys.
- **Build and run:** `gradle :desktop:run`. Installer: `gradle :desktop:packageMsi`.
- **Your data (step 3b):** playlists, favorites, play counts and the queue live in `%APPDATA%\KingPlay\user.json`, apart from the scanned library, so rescans and removing a folder never touch them. A play counts after half the song (or 4 minutes) or when it ends. The queue is saved every few seconds and on close; on the next start it comes back paused and the first play seeks to where you were.
- **Playlists:** create, rename, delete, add from the song menu, remove, export and import `.m3u`; smart lists (Favorites, Most played, Recently played, Recently added).
- **Import from the Android app:** Library page -> Import Android backup. Playlists and favorites are matched by folder and file name (only unambiguous matches); existing playlist names are skipped; nothing is overwritten. The M3U and path-matching code moved from `:data:library` to `:core:model` so both apps share it.
- **Look and feel (step 3c):** album covers from the song's embedded picture or `cover.jpg` / `folder.jpg` next to it (one per album, cached, decoded small); theme (match Windows, light, dark, black) and accent colour; the volume is remembered. Settings live in `%APPDATA%\KingPlay\settings.json`.
- **Backup export:** Settings -> Export backup writes playlists and favorites in the Android backup format, marked as a desktop backup, so restoring it on the phone adds them without touching the phone's settings. The Android restore now checks that mark (`BackupData.source`).
- Not yet on desktop: lyrics, EQ and effects, video, drag to reorder, tray and mini player, global media keys when the window is not focused, imported lyrics and EQ presets from a backup, play counts in the backup.

## Known limits (by design, tracked for later steps)
- Tag editing is not built yet. ReplayGain is read from the file while it plays; the library columns for it stay null.
- Subtitle files next to a video are not found automatically (scoped storage); pick the file with the system picker.
  A picked subtitle is not remembered after the app restarts, and adding one reloads the video at the same position.
- Playlists refer to MediaStore ids. If Android rebuilds its media index, ids change; a backup file relinks them by path.

- No search screen yet (`SearchRepository` exists).
- Playlist rename and drag-to-reorder are not built yet; the queue can be reordered with Move up / Move down.
- Device EQ profiles and the queue are not part of the backup (device keys are hardware-specific).
- Folders can be re-included in Settings but not newly excluded there (the system picker gives no plain path); use the folder screens.
- Genres and folders list songs only; an album grid view and per-tab sort options (other than Songs) come with polish in 2i.
- Fade is a fade out and in per song, not an overlapping crossfade (that needs a second player).
- The output device is guessed (USB, wired, Bluetooth, speaker) because Android before 13 does not say which is active.
- The EQ uses 16-bit or float output; bit-perfect / USB exclusive output is not part of this step.
- Lyrics are read only from tags inside the file. A separate `.lrc` file next to the song is not reachable under scoped
  storage; use "Import .lrc file" in Now Playing instead. Lyrics editing is not built.
- The visualizer is drawn from the audio just before it reaches the speaker buffer, so on Bluetooth it can lead the sound
  by a fraction of a second.
- The sleep timer lives in the playback service. It does not survive the app being killed.
- Video: no subtitle delay control, no background audio for video (it pauses when you leave, except in picture-in-picture),
  and no network streams.

## Rules
- Feature modules never depend on each other; they use `AppNavigator`.
- Room is the single source of truth (from step 2b).
- Analytics only through `AnalyticsTracker`, gated by `ConsentGate`.

## Add a feature module
1. Create `feature/<name>/build.gradle.kts` with `kingplay.android.library`, `kingplay.android.compose`, `kingplay.android.hilt`.
2. Depend only on `:core:*` modules. Include it in `settings.gradle.kts` and add it to `:app`.
3. Expose a `NavGraphBuilder` extension; navigate elsewhere through `AppNavigator`.

(How to add an audio effect is documented in step 2e, when the processor chain lands.)
