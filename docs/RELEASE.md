# Build and release Wrongulator

## Get a test APK
Open the repository on GitHub → **Actions** → **Android — checks and APK**.
After a **successful** run, download `wrongulator-debug-apk` from Artifacts.
Extract the ZIP and install `app-debug.apk` on your Android device (Android 8+).

Debug builds include a clearly marked **developer-only accurate preview**
inside the paywall. It does not bill and does not represent a real subscription.

## Local build
Requires JDK 17, Android SDK Platform 36, Gradle 8.13.

```bash
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

This source repository uses GitHub Actions to install a pinned Gradle version.
It does not yet include a binary `gradle-wrapper.jar` in Git. Before standalone
distribution, generate and commit the verified official Gradle Wrapper files.

## Before publishing a paid release
1. Create a permanent release keystore and back it up safely.
2. Configure and test Google Play Billing and verified subscriptions.
3. Remove debug feature access from release (already gated by BuildConfig.DEBUG).
4. Set real product price/renewal/cancellation disclosures and privacy policy.
5. Test on Android API 26/current with rotation, large fonts and low memory.
6. Check release APK signing, install/upgrade and store policy compliance.

**No current GitHub Release or paid subscription is implied by source code alone.**
