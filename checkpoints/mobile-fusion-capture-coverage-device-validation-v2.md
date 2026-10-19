# Mobile Fusion capture-coverage device validation v2

Date: 2026-10-18 Central  
Task: LOCAL-002  
Source: `3c692a7`  
Status: accepted through phone capture plus exact-evidence local re-fusion

## Correction

Three v1 phone records proved that tracker JSON persists non-truncated recognition-coverage intervals. Their fused artifacts nevertheless emitted the legacy “coverage was not measured” diagnostic because fusion checked generic source completeness, which deliberately remains unknown, rather than `MatchRecord.recognitionCoverage`.

V2 reports measured counts for `FRAME_OBSERVED`, `PLAY_DETECTION_ELIGIBLE`, and `CAPTURE_GAP`, plus truncation disclosure when applicable. It explicitly says coverage is observation eligibility, not proof that every gameplay action was recognized. Legacy records without telemetry retain the conservative unmeasured warning. Generic source completeness remains unknown.

## Verification

- 220 core tests across 36 suites: zero failures, errors or skips.
- SDK 36 `assembleFusionTest`: passed with cached Gradle 8.14.3 offline and fresh redirected outputs.
- APK: 68,318,573 bytes.
- APK SHA-256: `14862adaa3c6a3a12c64685379538a5edeaabc3a15290623c6bbab65390f14e8`.
- Package/version: `com.stroexd.hsdecktracker.fusiontestr4`, `1.5.0-fusiontestr4` (`10500`).
- Signer certificate SHA-256 unchanged: `4cf267d92ed361c32e2d4baa302670ee6c62a0cad042586e43de684ccc18f380`.
- APK Signature Scheme v2, ZIP alignment, and all eight native-library 16 KiB checks passed.
- REA summary evidence: `ev_2df86a792d5c035a26e59eac79120e6e9a6bcf547e1caa05812d5cba70ea8e57` (parent `ev_cb12b6799a8959ee8bb08c6a313fe38e40f65e37aa1bf27532909eded0d262aa`).
- REA class search: `ev_76992de557b6ee88e317c035e08eb428c28b1942fd39bd8aef3cb0e9e761f2a2`.
- REA class inspection: `ev_99c0800bbc14c2e8267831bfc90ac405946ece3f26caa401913399cfb2dd50d7`, confirming shipped `FusionCoordinator.trackerCoverageDiagnostics(TrackerEvidence)`.
- REA is static evidence only; it did not execute the APK or verify runtime behavior. Android tooling independently verified signing and alignment.

## Minimum phone gate

1. Update **HS Deck Tracker Fusion R4 Test** with this APK; do not uninstall or clear data.
2. Open one of the three already-completed instrumented records.
3. Re-run its saved-log review/fusion flow so a newly generated fused artifact uses the corrected coordinator.
4. Export/upload the new fused JSON. No new Hearthstone game is required.
5. Acceptance requires a measured coverage diagnostic with interval counts and no false “coverage was not measured” message.

No source push, merge, deployment, R6 work or HSC-006/010 hand reconstruction is authorized by this checkpoint.

## Acceptance result

The phone correctly preserved its existing immutable v1 fusion, so re-upload could not rebuild it. The exact phone-exported 7:48 PM Rogue/Priest tracker JSON and matching troubleshooting ZIP were therefore imported into a fresh local repository through the corrected core. Output retained logical fusion ID `50ea73800f48026f27927d6504ea0f1b`, contained 571 events, and reported 17 measured intervals: 9 frame-observed, 8 play-detection-eligible and 0 capture-gap. The false unmeasured warning was absent. Output SHA-256: `76bca958d93af95f27ed82b630eed6b01139b7a1e79a886a36412883dbfebc74` (local/private validation evidence, not published as a release artifact).

This accepts LOCAL-002 without deleting or rewriting phone history. It does not claim every action was recognized and does not close HSC-004 or HSC-006/010.
