# Wrongulator Android architecture

## Why one module?
This is a small **offline joke calculator**, not an enterprise application.
We deliberately keep one Android Gradle module with a pure-Kotlin calculation core.
When the feature set grows, extract `:core:calculator` and `:feature:calculator`;
don't add empty modules prematurely.

```text
MainActivity (single Android entry point)
  └── Compose WrongulatorScreen (stateless controls and paywall preview)
       └── CalculatorViewModel (owns observable screen state)
            └── CalculatorMachine (pure Kotlin state transitions)
                 └── CalculatorMath (BigDecimal arithmetic/parody strategy)
```

**Boundaries**
- A real paid entitlement must be verified outside the calculator engine.
- The engine never grants premium status and has no network / billing SDK.
- Production UI cannot toggle into the demo PRO mode. Debug-only preview uses BuildConfig.DEBUG.
- FREE always labels results as a deliberate joke. It must not present them as trustworthy.
- Zero internet, zero tracking, no dangerous Android permissions in v0.1.
- Division by zero is consistently an error, even in parody mode.
- Operands are capped at 12 digits; precision uses Java's DECIMAL64 for division.

**Reference foundations**
- Android official application architecture: https://developer.android.com/topic/architecture
- Android recommended architecture: https://developer.android.com/topic/architecture/recommendations
- Google Now in Android: https://github.com/android/nowinandroid
- Universal engineering principles from https://github.com/kabasik007/Appbootstrap/tree/android

Avoid runtime dependency injection frameworks, databases and background tasks
until a functional need justifies them.

## Quality
CI must compile, run local JVM unit tests for correctness and deliberate parody,
run Android Lint, and upload an APK build. Device/UI tests and Play Billing tests
will be necessary before a store release.

## Toolchain
- Kotlin 2.2.21 + Jetpack Compose / Material 3
- Android Gradle Plugin 8.13.2 / Gradle 8.13 / JDK 17
- compileSdk + targetSdk 36; minSdk 26
- Gradle version catalog in `gradle/libs.versions.toml`

## v0.2 presentation

A compact full-height Compose calculator anchors the 5-row keypad to the bottom and gives the result flexible height. A new incorrect evaluation triggers one keyframe wobble of the result panel before presenting a Material 3 modal bottom sheet with three price concepts. This is an **illustrative subscription selector**, not an active billing flow. Acknowledge and visually label the intentional parody result even when the screen has minimal copy.
