# Wrongulator Arcade OS — native mini-game architecture

## First playable release (v0.4 preview)

Four original Android-native mini-games:
- Snake (grid, food, score, wall/body collision)
- Block Drop (seven falling tetromino patterns, rotations, wall kicks, line clear, hard drop)
- Countryballs Dodge (original flag-inspired rolling-ball character, hazards, coins, shield)
- Moto Trail (original side-scrolling terrain, jump/gravity/braking/cone collision)

All assets are drawn procedurally with Compose Canvas. No engines, WebViews,
third-party sprites or online services are needed.

## Plug-in contract

`app/.../arcade/ArcadeGame.kt` defines `ArcadeGame`, `ArcadePainter` and `PadKey`.

A new built-in game is a **single Kotlin implementation file**:

```kotlin
class NewGame : ArcadeGame {
    override val width = 100f
    override val height = 100f
    override val intervalMs = 50L
    override val score = 0
    override val finished = false
    override fun reset() { /* reset state */ }
    override fun input(key: PadKey) { /* react to buttons */ }
    override fun tick() { /* one fixed logic step */ }
    override fun paint(painter: ArcadePainter) { /* draw primitives */ }
}
```

Register the factory and localized card metadata in `ui/ArcadeScreen.kt`:

```kotlin
ArcadeEntry("new-game", R.string.game_new, R.string.new_about, R.string.new_controls,
    "★", consoleAccent, { NewGame() })
```

The orchestrator instantiates **only the chosen game** and runs **one**
cancellable coroutine with a fixed, per-game step interval. No timer or game
loop is created for games that aren't on screen. Compose Canvas renders
normalized primitives and adapts to various phone widths. Android lifecycle
STOP pauses stepping. Exiting a game disposes and stops its coroutine.
High scores are saved per game using small local SharedPreferences keys.

## Limits and optimization principles

- All games currently compile into the single APK; not independently downloadable
  modules. Per-game files isolate code and lazy instance creation saves runtime
  memory, but **does not remove bytecode from the APK**.
- Fixed frame intervals are 45–470ms by game. No unbounded asset cache.
- No unnecessary offscreen rendering, images, network, microphone permissions or telemetry.
- For a large downloadable library later, use trusted, signed Android App Bundles
  and Play Feature Delivery, or safely sandboxed data-only game packs with verified
  manifests/assets. Do NOT load arbitrary downloaded .dex/.jar/.apk or execute
  unknown scripts with application permissions.
- Engine state is not currently saved on process death; only high scores persist.
- Input is tap buttons for now. Future work: swipes, gamepad/DPAD, accessibility
  announcements, rotation-aware fullscreen landscape, audio with mute preference.

## Future game shelf

2048, Breakout, Pong, Minesweeper, Sudoku, endless jumper, more Countryballs
modes, rhythm game, puzzle rooms. Original gameplay, names and art should be used
where a commercial game's brand or artwork would create rights concerns.

## Verification

Run Gradle tests, lint and debug APK build through GitHub Actions. On-device
latency, FPS, controller feel, process death and font scaling still need
hands-on testing. Avoid stating hardware performance without measurements.
