# Light Switch 💡

A simple logic game for Android, for kids (6+) and adults.

**Goal:** light up every lamp. Tapping a lamp switches it **and** its neighbours
(above, below, left, right). Finish in the fewest moves to earn ★★★.

- 4 difficulties: **Kid** 3×3, **Normal** 4×4, **Expert** 5×5, **Master** 6×6
- 30 levels each (120 total), unlocked one by one
- Undo, Restart and Hint buttons
- Sounds, vibration and confetti, no ads, no internet needed

## Install on your phone

1. On your Android phone, download [`apk/LightSwitch.apk`](apk/LightSwitch.apk)
   (on GitHub open the file and tap **Download** / "View raw").
2. Open the downloaded file. If Android asks, allow your browser or file app to
   **install unknown apps**.
3. Tap **Install**, then open **Light Switch**.

Requires Android 8.0 or newer.

## Build it yourself

Needs JDK 17+ and the Android SDK (platform 35, build-tools 35.0.0).

```bash
./gradlew assembleRelease          # -> app/build/outputs/apk/release/app-release.apk
./gradlew testDebugUnitTest        # level/solver tests + screenshots in app/build/screenshots
```

The app is signed with `app/lightswitch.keystore`, a hobby key kept in the repo so
new builds install as updates over the old one. Create a private key before
publishing on Google Play.

## Code

All code is in `app/src/main/java/com/firstmobileapp/lightswitch/`:

| File | What it does |
|------|--------------|
| `Puzzle.kt` | Game rules, a solver for the shortest solution (used for hints and the target), level generator |
| `MainActivity.kt` | Screens: menu, level select, game, win panel |
| `BoardView.kt` | Draws and animates the lamps |
| `ConfettiView.kt` | Win celebration |
| `SoundFx.kt` | Generated sound effects |
| `Progress.kt` | Saves stars and settings on the phone |
