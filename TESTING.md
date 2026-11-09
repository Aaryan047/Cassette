# Manual testing checklist

Run these checks on a physical Android phone when possible. Local files stored on the device are best for the first pass; cloud-backed document providers can add delays and permission issues.

## 1. Install and launch

- [ ] Sync Gradle and build the debug variant.
- [ ] Install and launch Cassette without a crash.
- [ ] Complete the first-run tutorial.
- [ ] Close and reopen the app; confirm the tutorial does not appear again unless replayed from Settings.

## 2. Music folder and library

- [ ] Select a folder containing a few audio files.
- [ ] Confirm tracks appear and are grouped by folder.
- [ ] Force-stop and reopen the app; confirm the selected folder is still available.
- [ ] Select a different folder and confirm the library updates.
- [ ] Try an empty folder, nested folders, and a folder containing non-audio files.
- [ ] Revoke folder access in Android settings, reopen the app, and confirm it asks for access again.

## 3. Playback

- [ ] Play, pause, resume, skip forward, and skip backward.
- [ ] Test shuffle and all repeat modes.
- [ ] Seek to different positions in a track.
- [ ] Confirm the visualizer clears while paused and while seeking, then resumes with playback.
- [ ] Lock the phone and confirm background playback and media controls work.
- [ ] Test Bluetooth headphones or a speaker.
- [ ] Test several file types separately (for example MP3, M4A/AAC, FLAC, and WAV). Support can vary by device and codec.
- [ ] Rename or remove a selected file after scanning; confirm the app shows an error instead of crashing.

## 4. Cassette deck

- [ ] Open the Cassette screen and confirm the phone switches to landscape.
- [ ] Check that the cassette, track title, seek bar, reels, and controls fit on screen.
- [ ] Test play/pause, previous/next, shuffle, repeat, and seek.
- [ ] Eject/leave the deck and confirm the app returns to its previous orientation and system-bar state.

## 5. Visualizer

- [ ] Play a track with strong bass and confirm the lower bands respond.
- [ ] Play a track with bright high-frequency sounds and confirm the upper bands respond.
- [ ] Confirm the bars bounce in place rather than scrolling from side to side.
- [ ] Try all styles: Thick Bars, Fine Bars, Squiggle, Mirror, and Dots.
- [ ] Change styles while the deck is open and confirm the selection applies.
- [ ] Pause, seek, and resume; confirm the visualizer stops and starts with playback.

## 6. Themes

- [ ] Test Carbon, Neon, Jade, Violet, and Gold.
- [ ] Test both Light and Dark appearance for each palette.
- [ ] Check text contrast and selected states on the library, folders, deck, and settings screens.
- [ ] Restart the app and confirm the selected palette and appearance are saved.

## Reporting a bug

Include the phone model, Android version, the steps that caused the issue, and whether it happens with every track or only a particular file. For playback errors, include the full error text shown by the app and the file type/codec if known.
