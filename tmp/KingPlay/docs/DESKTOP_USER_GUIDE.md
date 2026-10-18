# KingPlay for Windows: user guide

## Before you start

- Windows 10 or 11, 64-bit.
- **VLC, 64-bit**, installed from videolan.org. KingPlay uses VLC's sound engine; without it, nothing plays and the
  app tells you so. After installing VLC, restart KingPlay.
- The installer is not code-signed yet, so Windows may show "unknown publisher". Choose More info, then Run anyway.

## First run

1. Open **Library** and add the folders that hold your music (and videos). KingPlay reads them and never changes your files.
2. Songs appear under **Songs**, **Albums** and **Artists**. Videos appear under **Videos**.
3. Click a song to play. Use the bar at the bottom for play/pause, next/previous, shuffle, repeat, seek and volume.

## What you can do

| Want to | Where |
|---|---|
| Make a playlist | ⋮ on a song → Add to new playlist; or Queue → Save as playlist |
| Reorder songs | Drag the grip at the right of a row (Queue or a playlist), or ⋮ → Move up / Move down |
| See lyrics | Lyrics page. Sources, in order: lyrics you imported, a `.lrc` file with the song's name next to it, lyrics inside the file. Import .lrc on that page |
| Change the sound | Equalizer page: presets, 10 bands, preamp. "Save as preset" keeps your own |
| Watch a video | Videos page. Subtitles and Audio choices are under the picture. KingPlay offers to resume where you stopped |
| Keep music playing in the background | Settings → Window: "Keep playing in the tray when the window is closed". Open the mini player from there or from the tray menu |
| Use the keyboard's media keys | On by default; Settings → Window turns it off (takes effect on restart) |
| Open a file from Explorer | Right-click → Open with → KingPlay. It plays in the running KingPlay |
| Move playlists to or from your phone | Settings → Your data: Export backup / Import backup. Copy your music to the PC first: songs are matched by file name |
| Change the look | Settings: theme (match Windows, light, dark, black) and accent colour |

## Where your data lives

`%APPDATA%\KingPlay` (paste that into the Explorer address bar):

- `library.json`: the scanned songs and your folders. Safe to delete; KingPlay rescans.
- `user.json`: playlists, favourites, play counts, imported lyrics, saved equalizer presets, where you stopped in videos, the saved queue. **This is the one to back up** (or use Export backup).
- `settings.json`: theme, volume, equalizer, window options.

Uninstalling does not delete these. Delete the folder yourself for a clean start.

## If something is wrong

| Problem | Try |
|---|---|
| "Playback needs VLC (64-bit)" | Install 64-bit VLC (not 32-bit), then restart KingPlay |
| A song is missing | Library page → Rescan. Check the folder is added and the file type is supported (mp3, flac, m4a, aac, ogg, opus, wav, wma, aiff, ape, wv, mka) |
| A song will not play | KingPlay says so and moves to the next one. Check the file plays in VLC itself |
| Wrong or missing covers | Covers come from the song's own picture, or `cover.jpg` / `folder.jpg` next to it |
| Lyrics not shown | Put `Song name.lrc` next to `Song name.mp3`, or use Import .lrc on the Lyrics page |
| Media keys do nothing | Another program (a browser, another player) may have claimed them first. Close it and restart KingPlay |
| "Open with" did nothing | KingPlay may be stuck from an earlier crash. Close it from Task Manager and try again |
| The tray icon is not visible | Windows hides new icons under the ^ arrow in the taskbar; drag it out |
| Equalizer seems weak | The sound engine's bands are close to, not exactly on, the labelled frequencies, so presets are approximate |

## Not included

Streaming and online lyrics or covers (everything is local), and syncing with the phone beyond backup files.
