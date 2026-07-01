# TaxCalc Pro

Modern Android Sales Tax & Reverse Sales Tax Calculator

Fancy Material 3 app with dark/light/dynamic color themes, per-county editable tax rates, full persistent calculation history (Room), visual breakdowns, quick amounts, and GitHub Actions that builds installable APKs on every push or manual trigger.

## Features
- Forward & Reverse calculators
- Manage any county/jurisdiction with custom rates (preloaded realistic 2026 US rates)
- Star favorites
- Every calculation auto-saved to history with snapshot of rate used
- Load history items back into calculator
- Searchable history & counties
- Animated tax proportion visual bar
- Summary stats (total calcs + total tax tracked)
- Haptics + spring animations
- Fully offline

## How to build the APK (easiest way)

1. Go to **Actions** tab in this repo
2. Select the **Android CI/CD** workflow
3. Click **Run workflow** (green button)
4. Choose branch `main`
5. Click **Run workflow**
6. When finished, download the `app-debug-apk` artifact
7. Install on your phone (allow unknown sources)

The workflow now uses `gradle/actions/setup-gradle` so it works reliably without needing a committed gradle wrapper.

## Local development (Android Studio recommended)

1. Clone the repo
2. Open in Android Studio
3. It will prompt to sync / generate wrapper if needed
4. Or run in terminal: `gradle wrapper --gradle-version 8.9` then `./gradlew assembleDebug`

Built raw and complete. No half-measures.
