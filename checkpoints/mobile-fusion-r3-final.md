# R3 final source/build checkpoint

Date: 2026-10-09  
Branch: `feature/mobile-fusion-offline`  
Boundary: ready for isolated physical-phone validation; no R4-R6, merge or deployment

## Completed R3 capabilities

- Separate debug identity: `com.stroexd.hsdecktracker.debug`, version `1.5.0-debug`, separate app-private data.
- Durable completed-match outbox with idempotent local persistence and explicit transfer states.
- Active-match draft journal with stable completion identity and stale-draft suppression.
- Explicit multi-bundle post-match evidence selection, conservative fusion, atomic local artifact persistence and a separate fusion outbox.
- Persisted saved-match status with retry-by-reselection for pending, interrupted, inaccessible or malformed evidence.

## Failure boundaries proven locally

- Process interruption before/after completed-match persistence does not duplicate a match.
- A completed outbox entry prevents an old active draft from reopening.
- Abandoned RECEIVED/PROCESSING fusion status becomes FAILED/retryable at startup.
- Security/permission denial and missing document handles are reported as bundle-access failures.
- Failed fused-artifact replacement deletes staging output and preserves the previous artifact.
- FUSED_LOCAL never means remotely verified.

## Final gates

- Ordinary core suite: 123 tests passed.
- Private fixture suite: 6 tests passed; private evidence stayed outside Git.
- Debug compile and APK assembly: passed.
- APK package: `com.stroexd.hsdecktracker.debug`.
- APK version: `1.5.0-debug`.
- APK debuggable: true.
- APK bytes: 68,824,434.
- APK SHA-256: `321ea959e0a08b5a10dfd58e80bf3c3ca92f28950f5a024005e13c3dbe77a361`.

## Device handoff

Local delivery directory:

`C:\Users\Xs_da\Documents\SoxCoachExporter\Matches\mobile-fusion-r3-device-validation`

Contents:

- `mobile-fusion-r3-debug.apk`
- `mobile-fusion-r3-debug.apk.sha256`
- `README-phone-validation.md`

After the user explicitly authorized the external copy, the complete package was also delivered to:

`H:\My Drive\HSReplay\Development\mobile-fusion-r3-device-validation`

The copied APK was reread from Drive and matched the recorded SHA-256. The local package remains available as a second copy.

Physical-device behavior remains unverified until the user completes the checklist. After results are reviewed, surface the R4 goal and prerequisites explicitly; do not transition automatically.

Five-hour usage was 32% at the final pre-commit check against the user-authorized 60% ceiling.
The post-push live reading was 36%.

## Corrected device identity — active v2 handoff

The preceding `.debug` delivery is preserved for history but is **superseded for this phone**. At the first device gate, Android offered **Update** rather than **Install**, proving that the working installation already uses `com.stroexd.hsdecktracker.debug`. The user cancelled before installation, so no known working-app data was modified.

The active phone-validation build is now:

- APK: `mobile-fusion-r3-fusiontest.apk`
- Package: `com.stroexd.hsdecktracker.fusiontest`
- Visible label: **HS Deck Tracker Fusion Test**
- Version: `1.5.0-fusiontest`
- Debuggable: true
- Bytes: 68,036,586
- SHA-256: `9744f913f7c40aaf7e4cbc1e2833a95cccb12e6eaf626f72be8df5a5ab8805d2`

Active v2 delivery directories:

- Local: `C:\Users\Xs_da\Documents\SoxCoachExporter\Matches\mobile-fusion-r3-device-validation-v2`
- Synced Drive: `H:\My Drive\HSReplay\Development\mobile-fusion-r3-device-validation-v2`

Android must offer **Install**, not **Update** or **Replace**, for the v2 APK. Physical validation remains pending, and R4 remains unstarted.

The final corrective gate passed `:core:test` and `:app:assembleFusionTest`. The final APK metadata and both local/Drive copies were independently reread after assembly; the Drive copy matched the recorded byte count and SHA-256.

## Device validation progress

The corrected v2 APK installed successfully as a new app. The user confirmed that **HS Deck Tracker Fusion Test** showed no historical decks or games from the working tracker, so the dedicated identity and separate app-private data boundary are phone-verified.

Still pending before R3 acceptance: reopen and confirm the original tracker's expected data, exercise active-match force-stop recovery, finish/reopen without duplication, and test evidence-import cancellation/retry plus persisted status. R4 remains unstarted.
