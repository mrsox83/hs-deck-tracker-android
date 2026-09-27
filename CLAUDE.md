# HS Deck Tracker – notes for Claude Code

Hearthstone deck tracker for Android that reads the game screen on the device. Public repo, GPL-3.0.

## Layout and commands

- `core/` – pure Kotlin/JVM: deck codes, cards, collection, meta, stats, persistence and the screen recognition state
  machine (`core/vision`). All logic that can be tested without a device goes here, with tests.
- `app/` – Android app (Compose, Material 3): overlay, screen capture (screen sharing or the app's own accessibility
  service), ML Kit text recognition.
- `./gradlew :core:test` – unit tests; `./gradlew :app:assembleDebug` – debug APK (needs the Android SDK).
- Replay a recorded diagnostics session:
  `HS_DIAG_DIR=<session> HS_CARDS_DIR=<dir with cards.enUS.json/cards.deDE.json> ./gradlew :core:test --tests '*DiagnosticsReplay*'`
  Never commit users' diagnostics (they contain player names).

## How the owner wants to work

- Work on a branch, open a pull request, merge it into `main` once CI is green – always, without asking.
- Commit messages and code in English; the owner writes German.
- No comment bloat and no explanatory filler text in the UI; keep texts short.
- Every UI text in `app/src/main/res/values/strings.xml` and `values-de/strings.xml`; the app follows the language of the
  Hearthstone client.
- No root, Shizuku or helper apps. Google Play was dropped on purpose.
- Nothing about promotion goes into the repo (posts, community lists, drafts); hand it to the owner directly.
- Recognition can't be tested on a device here: cover changes with core tests or a diagnostics replay and say so.

## Releases and distribution

- Raise `versionName` in `app/build.gradle.kts`; merging that into `main` runs `.github/workflows/release.yml`: signed APK
  as GitHub release (`releases/latest/download/hs-deck-tracker.apk`), F-Droid repository rebuilt on the `fdroid` branch.
  Obtainium reads the GitHub releases. Details: `docs/RELEASING.md`.
- The signing keys are `signing/keys.p12.enc`, encrypted with the `SIGNING_PASSWORD` secret. Never replace them – new
  keys break updates for every user.
- Feedback arrives as GitHub issues (forms in `.github/ISSUE_TEMPLATE`, buttons in Settings → About).
