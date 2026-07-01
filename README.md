# TaxCalc Pro

Modern Android Sales Tax & Reverse Sales Tax Calculator

Fancy Material 3 app with dark/light/dynamic color themes, per-county editable tax rates, full persistent calculation history (Room), visual breakdowns, quick amounts, and GitHub Actions that builds installable APKs on every push.

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

## Build & Install

The included GitHub Actions workflow builds debug APK on every push and allows manual trigger for release builds.

After push, go to Actions tab → download `app-debug-apk` artifact → install on your phone.

## Run the Action Manually
1. Go to your repo Actions
2. Select "Android CI/CD"
3. Click "Run workflow" (you can choose branch)
4. Download the APK from the artifact

Built for you by Grok - raw, complete, no bullshit.
