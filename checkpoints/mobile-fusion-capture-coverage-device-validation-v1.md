# Mobile Fusion capture-coverage device validation v1

Date: 2026-10-18 Central  
Task: LOCAL-002  
Status: APK/static gates passed; phone acceptance pending

## Scope

This package prospectively validates the capture-coverage containment for October 17 coaching Bugs 1 and 2. New completed records can distinguish `FRAME_OBSERVED`, `PLAY_DETECTION_ELIGIBLE`, and `CAPTURE_GAP` intervals without putting coverage bookkeeping into the gameplay-event stream. It does not reconstruct missing history in older records and does not include the separate HSC-006/010 hand-reconstruction proposal.

## Source and build

- Branch: `feature/mobile-fusion-offline`.
- Source commits: `6987fe9` (unknown completeness containment), `ba1f5f1` (bounded coverage intervals), `4c0cdf7` (raw review completion).
- Build: cached Gradle 8.14.3, SDK 36, offline, fresh redirected outputs.
- Tests: 219 tests across 36 suites; zero failures, errors or skips.
- APK: `app-fusionTest.apk`, 68,317,765 bytes.
- APK SHA-256: `faba362a32f3e312cfefc3838796bfd92ebeda6505b066259940fd5f91dcbcd8`.
- Package: `com.stroexd.hsdecktracker.fusiontestr4`.
- Version: `1.5.0-fusiontestr4` (`10500`).
- Label: `HS Deck Tracker Fusion R4 Test` (known low-priority stale test-build label).
- Signer certificate SHA-256: `4cf267d92ed361c32e2d4baa302670ee6c62a0cad042586e43de684ccc18f380`.

## Artifact gates

- APK Signature Scheme v2 verification passed.
- ZIP alignment passed; all eight packaged native libraries passed 16 KiB alignment.
- Manifest/package/version/SDK/permission inspection matched the expected test identity and permission set.
- REA summary evidence: `ev_c3dbedac5dcf50346dd37bc728e667e7df617983ad987943b9f15ae9a5e7712f`.
- REA fusion search evidence: `ev_97e30a684164dce0870ff5d74a16a91efc218b40f484b23e2b6acce98a6d3070`.
- REA coverage class evidence: `ev_a7db24db52513260a98d0020b42d5b0fd8120bf6c8116aa75b249d4a8233878a`, `ev_846509e5351943b31679ce19f9591117763d5d039b415c90953e1b6d827b43fd`, `ev_a47f357e7a80b3b1465046958871e98a683187d11d676a50efbd1fe1365e6b07`.
- REA is static evidence only: the artifact was not executed, and runtime/phone behavior remains unverified.

## Minimum phone gate

1. Install this APK as an update to **HS Deck Tracker Fusion R4 Test**. Do not uninstall either tracker app and do not clear app data.
2. Confirm the existing Fusion R4 Test history is still present.
3. Track and complete one new match with the normal capture/overlay workflow. Historical records are not valid for this gate.
4. Export the newly completed tracker JSON. If a natural capture interruption occurred, note approximately when; do not manufacture a disruptive failure solely to create a gap.
5. Upload that new JSON for comparison. Acceptance requires coverage metadata that reflects observed/eligible/gap intervals conservatively, while the gameplay timeline still opens and replays normally.

## Boundaries

LOCAL-002 remains REVIEW until the phone result is inspected. This checkpoint does not authorize a source push, merge, deployment, R6 work, or HSC-006/010 hand reconstruction.
