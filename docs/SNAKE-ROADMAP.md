# Snake Combat — game roadmap

**Updated:** 2026-10-10 · **Target:** Wrongulator Arcade v0.6+

Keep the core Snake game independently testable, offline-first and playable
with one thumb, two thumbs, touch D-pad, swipe or optional hardware controller.
Do not load untrusted executable plug-ins or bloat the APK with sprite libraries.

## Implemented in v0.5
- [x] Classic grid snake, reliable reverse-direction prevention and wall/body collisions.
- [x] Progressive speed, scores, best score persisted locally.
- [x] Interpolated movement, glowing food, sparkles and expressive snake eyes.
- [x] Dedicated responsive screen, start/pause/resume/restart.
- [x] D-pad, directional swipes, keyboard/gamepad, mute, retro sound and haptics.
- [x] Background pause and disposal-safe game loop.

## v0.6 — Snake Combat
- [x] Fictional **grenade launcher** (💥 FIRE) separate from main START/PAUSE button.
- [x] Destructible neon crates, directional 8-cell projectile, 3×3 cartoon explosion.
- [x] Ammo counter, 3 initial grenades, +1 per two food pickups (cap 5).
- [x] Obstacles grow gradually every third food pickup, maximum 12.
- [x] +15 score per destroyed crate; clear miss/no-ammo behavior.
- [x] Animated blast flash, sparks, retro firing sound, haptics.
- [x] Hardware gamepad B/R1 fires; existing A/Enter controls main action.
- [x] Pure Kotlin tests for ammo, collisions, destruction, reset, safe rendering.

## Candidate mechanics to prioritize
1. **Combo meter:** fast consecutive pickups boost score and trigger neon trails.
2. **Power-ups:** temporary shield, magnet for nearby food, slow motion, double score.
3. **Grenade variants:** one bouncing projectile and one multi-crate chain reaction.
4. **Challenge missions:** survive 60 seconds, destroy 10 crates, reach length 30.
5. **Modes:** Classic (without weapons), Combat, Zen (no death), Time Attack.
6. **Difficulty:** Casual/Normal/Expert with independent best scores.
7. **Boss encounters:** a large moving arcade drone with destructible shields.
8. **Enemies:** 1–3 bounded rival snakes with local non-blocking AI.
9. **Skins:** colors, eyes and trails earned by gameplay; accessibility-safe palettes.
10. **Maps:** tunnels, portals, wrap-around arena, hazards with clear indicators.
11. **Local achievements:** score, survival, streaks, crates, no-hit runs.
12. **Practice/tutorial:** first-play guide and a 5-second safe countdown.
13. **Better audio:** tiny synthesized SFX using SoundPool when audio assets warrant it.
14. **Controller accessibility:** adjustable handedness, remappable buttons, visual haptic toggle.
15. **Battery/quality:** 30/60 FPS options based on measured phone performance.
16. **Replays:** deterministic seed + compact input-log replay, opt-in shareable score card.
17. **Portrait/landscape:** independently tested responsive layouts without shrinking controls.
18. **Online leaderboard:** only with opt-in and anti-cheat; offline always playable.

## Product rules
- A grenade is strictly **cartoon in-game**. No real-world weapon information.
- Destruction only affects special crates, not the snake body or physical world.
- Ammo/spawns must be bounded; no background polling or analytics.
- Real FPS, accessibility and input feel require manual testing on physical Android phones.
- Source is in `arcade/SnakeGame.kt`; UI is in `ui/SnakeConsole.kt`.
- Before merge: run JVM unit tests, Android Lint and APK assemble via GitHub Actions.


## v0.7 implementation audit (Snake Ultimate)

New pure-Kotlin systems:
- `SnakeRules.kt`: four modes, three difficulty levels, four arenas, four skins, selectable enemy count
- `SnakeWorld.kt`: capped rival AI (up to three), moving multi-hit boss, barriers, map hazards, bounce/chain/grenade shots
- `SnakeGame.kt`: combo multiplier, four bounded timed powers, food magnet, shields, slow motion, double score, timed runs and mission counters
- `SnakeReplayCodec.kt`: compact versioned, URL-safe replay data with strict size bounds and non-executable input events
- `SnakeVisuals.kt`: procedural power-ups, opponents, boss, portals, glowing trails and alternate palettes
- UI split into `SnakeUltimateConsole`, `SnakePlayfield`, `SnakeControlPanel`, `SnakeSettingsSheet`, `SnakeProgressSheet` and `SnakePreferences`

Implemented: ideas 1–17 in locally testable scope, with device-QA caveats for frame rate, landscape, replays and SoundPool. For #13, a new SoundPool bank generates tiny original WAV clips in app cache on a background dispatcher and falls back to the previous native tones while buffers load; perceived latency and mixing still require real-device audio QA. For #18, **local** best scores exist; an actual online leaderboard is **not present** and will not be claimed until an opt-in remote API, moderation/rate limiting, consent, and anti-cheat verification are deployed.

The reproducible replay is a seed and compact input trace. Share is opt-in through Android's standard share sheet. It is not a video capture feature. Native 30/60 FPS selection throttles visual repaints; it does not change the simulation clock. Landscape uses an alternative board + side-controls layout. Phone/controller/device QA and replay mismatch edge cases remain to be verified.


## v0.7.1 — Android phone usability hotfix

Reported using an actual Android screenshot: the 5-second `Get ready` dialog
blocked input, directional tiles appeared disabled, duplicate parent `Ігротека`
toolbar wasted vertical space and the board was too small.

Implemented:
- [x] Begin automatically after a single ~900 ms visual transition; no 5-to-1 countdown or `Get ready` overlay.
- [x] Big bottom-right START skips warmup immediately, then becomes PAUSE/RESUME/RESTART.
- [x] Arrows accept touches even during transition, with a two-turn queue validated by pure JVM tests.
- [x] Larger (at least 54 dp) touch areas, brighter arrows, clickable center PAUSE key.
- [x] D-pad/screen swipe callbacks always refer to current game state.
- [x] Full-width controller row, avoid accidentally measuring the board against a full-height sibling.
- [x] Hide redundant parent Arcade header during active game; remove nested insets from Snake.
- [x] Give remaining vertical space to the portrait game field; compact HUD and board margins.
- [x] No new permissions or heavy artwork.
- [ ] On-device touch, talkback, multiple resolution, controller latency and FPS QA still required.
