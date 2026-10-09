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

At that point, the remaining R3 device checks were to reopen the original tracker, exercise active-match force-stop recovery, finish/reopen without duplication, and test evidence-import cancellation/retry plus persisted status. R4 remained unstarted.

The user subsequently reopened the original tracker and confirmed that its decks and match history remained present and active. The installation/data-isolation portion of R3 physical validation is complete. Remaining device work is active-match recovery, completion deduplication and evidence-import status/retry validation; R4 remains unstarted.

### Force-stop/relaunch evidence

The user played turn 1, stopped during the opponent turn, relaunched and continued. `HS-export-20261009-155235-2708d9c1.zip` contains an incomplete pre-stop fragment (`Hearthstone_2026_10_09_15_42_59`, source SHA-256 `4fc1dccd08cb4d01a4f5aaba8441cb888f0c2c885640164f1eafe33f0502dd7d`) and a completed post-relaunch fragment (`Hearthstone_2026_10_09_15_45_23`, source SHA-256 `4e82739d5fa44b9a70ff6e690836467ccbd298a81b2c06d8fd48703c9087194a`). They are one logical validation match. The additional `Hearthstone_2026_10_09_13_13_55` entry is a null-descriptor `READ_FAILED` record, not a decoded game.

The bundle is marked **FOR TESTING** through the companion `.testing.json` file and Shared Exchange routing note, excluding it from coaching. Tracker-side confirmation of exactly one completed match/no reopened draft and the evidence-import retry/status checks remain pending. R4 remains unstarted.

Phone screenshots confirm one automatic completed tracker record with six turns and a 6:27 duration. Its timeline retains the pre-stop Start events and shows Turn 3 onward, but has no Turn 2 group. Since the UI renders only turns containing persisted timeline events, this is a bounded recognition gap rather than evidence that the draft restarted; the duration closely spans the complete two-fragment exporter interval. A subsequent app restart must still verify that the finished draft does not reopen. Diagnostics were off and cannot recover past OCR/screenshots; enable them only for a deliberate reproduction if the missing-turn gap needs deeper diagnosis.

### R3 physical acceptance

The on-device evidence-import flow is accepted. Picker cancellation made no success claim. Selecting `HS-export-20261009-155235-2708d9c1.zip` produced **Fused artifact saved locally**, and reopening the fusion-test app retained that result. This validates the real two-fragment continuation bundle against the recovered tracker match, local fused-artifact persistence and visible status persistence.

R3 is now source-ready, tests-passed, fusion-test APK-built, phone-verified and pushed. It is not merged or deployed. The documented Turn 2 recognition gap is bounded to the intentional interruption and successfully enriched by fusion. Automated gates cover forced access/retry/interruption failures not manufactured again on the phone. R4 remains unstarted pending explicit authorization.
