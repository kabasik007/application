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

## v0.5 Snake Console 2.0

Snake now launches inside a dedicated premium-styled screen while sharing the common `ArcadeGame` contract and central `drawArcadeFrame` renderer. The game is **READY** on opening, and the large bottom-right action button starts play. While playing, the same button pauses; paused play resumes with it; GAME OVER turns it into restart. The overlay mirrors those actions. Touch D-pad and directional swipes are supported, as are keyboard / external DPAD directional keys and A/Enter/Space. Navigation back stops the game loop.

The pure Kotlin `SnakeGame` keeps previous and current grid coordinates and interpolates visually at a vsync-paced rate while the logical movement stays on a fixed interval (155ms down to 78ms with score). Food has subtle pulse/spark effects, the head has directional eyes, and score animates on collection. The render interpolation does not change game logic.

Audio uses a small Android-native ToneGenerator wrapper (no network or bundled files) for start, pickup, game-over and pause. Players can mute it using `♪ ON/OFF`; their preference and high scores stay on the device. Haptic feedback is used on controls. On backgrounding, Snake pauses and **does not automatically resume** when the app comes back. Only the active game runs an update loop. No changes to other game engines.

Performance limitations: visual animation follows the display refresh rate only while playing; offscreen/paused loops stop. Native touchscreen feel, battery, audio latency, screen reader behavior and devices of varying aspect ratio require on-device QA; passing CI proves only compile/tests/lint, not frame-rate targets.


## v0.6 — Snake Combat

Snake gets an **original cartoon grenade launcher**: a distinct, amber FIRE button above START/PAUSE, a 3-grenade magazine, +1 grenade after every 2 food items (maximum 5), +15 score per destroyed destructible crate, and one extra crate spawned after each 3 food items up to a bounded 12. The direction is the last executed movement; the grenade flies up to 8 grid cells, stops on a crate and destroys crates inside a 3×3 region. Misses expend ammunition. The original head/tail mechanics, collision, pause and scoring remain independent from the Android UI.

The explosion uses procedurally drawn particles and visual fade ticks; firing plays a synthetic sound and haptic cue. Bluetooth/USB controller BUTTON_B or R1 fires. No additional permissions, downloaded files or large sprite sheets. See [Snake roadmap](SNAKE-ROADMAP.md) for tested behavior and future ideas.
