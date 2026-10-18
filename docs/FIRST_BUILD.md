# First build: what to expect

Most of this code was written and tested without a full Gradle build. The shared logic (`:core:model`, and the pure
parts of `:desktop` such as queue, library, backup, settings, lyrics, video scanning) was compiled and unit tested on its
own. The screens and everything that talks to Android, Compose, VLC, jaudiotagger or JNA was **not compiled anywhere**.
Expect a handful of compile errors on the first build. This page says where, in the order to tackle them.

## Order

1. Get Gradle: `gradle wrapper --gradle-version 8.10.2`, then commit the wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/`).
2. Needs JDK 17. Check with `java -version`.
3. Desktop, smallest first:
   - `./gradlew :core:model:test` (pure; should pass at once)
   - `./gradlew :desktop:test` (pure logic and the tests; should pass once the module compiles)
   - `./gradlew :desktop:compileKotlin` (this is where the screen code is first checked)
   - `./gradlew :desktop:run`
4. Android: `./gradlew :app:assembleDebug`, then `detekt` and `lint`.
5. Style checks last: `./gradlew ktlintCheck detekt`. Run `./gradlew ktlintFormat` first to clear formatting-only findings.

If a Gradle error mentions a plugin or a version, fix that before reading any Kotlin errors; one configuration
problem hides everything after it. (One was already found this way: a version-catalog alias ending in `-class` is
reserved by Gradle and stopped the build from configuring. It is renamed.)

## Desktop: the likeliest compile errors

| Where | What I am least sure of | What to do if it fails |
|---|---|---|
| `engine/VlcjAudioEngine.kt`, `ui/VideoPlayer.kt` | Exact vlcj 4.8.3 method names: `media().play(mrl, options)`, `controls().setPause/skipTime/setTime`, `audio().setVolume/trackDescriptions/setTrack`, `subpictures().trackDescriptions/track/setTrack/setSubTitleFile`, `equalizer().newEqualizer()`, `audio().setEqualizer` | Check the vlcj 4.8 javadoc for the same API group (`MediaApi`, `ControlsApi`, `AudioApi`, `SubpictureApi`). The names are standard; a parameter type may differ |
| `ui/VideoPlayer.kt` | `EmbeddedMediaPlayerComponent` inside `SwingPanel` | If the picture stays black or the window flickers, try `compose.interop.blending` or play the video in a plain Swing `JFrame` |
| `Main.kt` | `Tray`, its menu `Item(...)`, `Window(visible = ...)` | Signatures are Compose Multiplatform 1.7.1; compare with its API docs |
| `input/WindowsMediaKeys.kt` | JNA structure layout for the Windows `MSG` and `RegisterHotKey` | Only used on Windows. If `Native.load` throws, the app carries on without media keys. Check the log |
| `ui/Reorder.kt`, `ui/Library.kt`, `ui/App.kt` | `detectDragGestures`, `graphicsLayer`, `zIndex` usage | Small import or parameter fixes |
| `ui/*` | Icon names from `materialIconsExtended` (`Lyrics`, `Equalizer`, `Forward10`, `Replay10`, `OpenInFull`, `DragHandle`, `Movie`, `FullscreenExit`) | Swap for an icon that exists in your version |
| `lyrics/EmbeddedLyricsReader.kt`, `library/JAudioTaggerReader.kt` | jaudiotagger 3.0.1 API (`FieldKey.LYRICS`, `AudioFileIO.read`) | Check the library's javadoc |
| `Theme.kt` | Material 3 `ColorScheme` parameter names | Compare with the Material 3 version bundled by Compose Multiplatform 1.7.1 |

Things that are logic, not syntax, and only show when you run it (worth a quick manual pass):

- Play a song, pause, seek, skip, and let one finish. Then quit and reopen: the queue should come back paused at the same place.
- Add a folder with music, a `cover.jpg`, a `.lrc` next to one song, and one video.
- Equalizer: move a slider while a song plays; it should change the sound.
- Close the window with "Keep playing in the tray" on; the music should continue and the tray icon should bring the window back.
- Press the keyboard's play/pause key with another window in front.
- Drag a song in the Queue and in a playlist.

## Android: the likeliest problems

| Where | What | Note |
|---|---|---|
| `PlaybackService.kt` | `onConnect` with `ConnectionResult.reject()` and `AcceptedResultBuilder`, `controller.isTrusted` | Media3 1.5.0. If `reject()` has another name, `AcceptedResultBuilder` alone with an `isTrusted` guard is enough |
| Glance widget (`NowPlayingPublisher` binding) | Glance API versions | Widget is optional; if it blocks the build, remove its binding and the widget receiver first |
| Android Auto | Cannot be tested without a car or the Desktop Head Unit | Install the Desktop Head Unit from the SDK manager to test the browse tree and voice search |
| Hilt / KSP | Annotation processing errors can hide the real cause | Fix the first error only, then rebuild |
| Room | Schema export and the version 2 migration | Run the instrumented test `:core:database:connectedAndroidTest` on a device or emulator |

## When you paste errors back

Send the first 3 to 5 errors only (later ones are often the same problem repeated) along with the command you ran.
That is enough to fix a whole file at a time.
