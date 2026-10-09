# WRONGULATOR ≠

> **Your math. Our opinion.**  
> Android meme calculator that charges for the revolutionary concept of correct arithmetic.

| Mode | 2 + 2 | Status |
| --- | --- | --- |
| FREE | **5** | Intentionally wrong · clearly labeled parody |
| PRO | **4** | Planned **$1/month** subscription |
| PRO debug preview | **4** | Testing only; no real payment |

![Android](https://img.shields.io/badge/Android-8%2B-3DDC84) ![Kotlin](https://img.shields.io/badge/Kotlin-2.2.21-7F52FF) ![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4)

## Status — v0.1 demo

✅ Native Kotlin / Compose UI with dark, meme-themed keypad and **≠** app icon.  
✅ Arithmetic engine using `BigDecimal`, with deterministic wrong answers in FREE and correct arithmetic in PRO.  
✅ Handles decimals, sign, percent, delete, division by zero, input length; unit tests.  
✅ English and Ukrainian text.  
✅ GitHub Actions workflow for unit tests, lint and a downloadable **debug APK**.  
⚠️ **The $1/month paywall is a demo only. No checkout, paid subscription, or billing backend is connected.**  
⚠️ GitHub Actions build passing and on-device verification have not been confirmed yet.

## Download APK for your phone

1. Open [Actions → Android — checks and APK](../../actions/workflows/android-ci.yml).
2. Choose a **successful** workflow run, download artifact `wrongulator-debug-apk`.
3. Unzip and install `app-debug.apk`. Android may ask to allow app installs from this source.

Debug builds have a **developer-only "Try accurate mode"** button in the mock paywall. A release build must never unlock paid mode without a verified entitlement.

## Local build

Requirements: Android SDK Platform 36, JDK 17 and Gradle 8.13.

```bash
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

The workflow installs pinned Gradle. A binary Gradle Wrapper is not yet bundled.

## Engineering docs

- [Architecture](docs/ARCHITECTURE.md): small, pure calculator core + Compose/ViewModel.
- [Subscriptions](docs/SUBSCRIPTIONS.md): honest pricing, Google Play Billing and entitlement security roadmap.
- [Release](docs/RELEASE.md): APK and release requirements.
- [AGENTS.md](AGENTS.md): coding and AI assistant contract.

### Subscription note

Real Google Play subscriptions require a properly configured Play Console monthly product, localized pricing, secure purchase verification, cancellation/restore support, and compliance review. They **cannot** be made real simply by writing "$1/month" on a button. The app discloses that its free results are deliberate jokes.


## v0.3 — drawer & genuinely useful tools

Use the ☰ icon (or swipe from the left edge) to open the side drawer. It has three pages:

- **Calculator:** our deliberately inaccurate free meme calculator, presented with a clean story-friendly layout and a subtle `FREE · NOT ACCURATE` label.
- **Smart Tools:** genuinely accurate discount (price + %) and split-bill (bill + tip + guests) calculators. Always free and fully offline.
- **About:** transparent explanation of the satirical calculator and mock billing.

The FREE result still triggers the animated mock subscription offer. There are no real purchases. The right side of the UI remains compact enough for portrait videos.


## v0.4 — Arcade OS: your pocket console

Open the left drawer → **Arcade / Ігротека**. Play four complete native mini-games:

| Game | Controller | Goal |
| --- | --- | --- |
| 🐍 Snake | ▲ ▼ ◀ ▶ | Collect food and grow without hitting walls |
| ▦ Block Drop | ◀ ▶ move, ▲ rotate, ▼ lower, ● hard drop | Clear horizontal lines |
| 🌐 Countryballs Dodge | ◀ ▶ move, ● protective shield | Dodge falling balls, collect gold |
| 🏍 Moto Trail | ▶ throttle, ◀ brake, ● jump | Survive obstacles and score distance |

All games have a pause button, restarts, real score and a **local high score**.
Games are offline, ad-free and play instantly with procedural graphics.

**Architecture:** Each game is its own Kotlin source file implementing the
shared `ArcadeGame` interface. The arcade orchestrator constructs only the
selected game, runs one lifecycle-aware loop and uses a single Compose Canvas
renderer. Adding a new game means a new file plus a catalog registration,
not another launcher, drawing engine or UI. See [Arcade architecture](docs/ARCADE.md).

**Scope:** All four implementations are compiled into the current APK. This
version does not load arbitrary external game code or download APK plugins.
