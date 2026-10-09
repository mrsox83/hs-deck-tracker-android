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

The requested synced-Drive delivery was not performed because the environment rejected the external upload without explicit authorization. The local package is complete.

Physical-device behavior remains unverified until the user completes the checklist. After results are reviewed, surface the R4 goal and prerequisites explicitly; do not transition automatically.

Five-hour usage was 32% at the final pre-commit check against the user-authorized 60% ceiling.
