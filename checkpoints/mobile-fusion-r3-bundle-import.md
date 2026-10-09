# R3 checkpoint 3 — selected post-match evidence import

Date: 2026-10-09  
Branch: `feature/mobile-fusion-offline`  
Scope: R3 post-match ingestion only; no R4-R6, merge or deployment

## User path

From a saved match detail screen, the debug app can open Android's multi-document picker and read one or more exporter ZIP bundles. The existing production tracker installation and its data remain untouched because this work stays in `com.stroexd.hsdecktracker.debug` and app-private storage.

## Conservative pipeline

1. Hash and persist the selected bundle identities before processing.
2. Parse exporter manifests and match documents with the existing R0 adapter.
3. Reduce every distinct Power source and evaluate only the strict existing reconnect rule.
4. Evaluate pairing for evidenced player controllers using endpoint, result, first-player and ordered-card evidence.
5. Accept only one unique source; otherwise persist PENDING_PAIRING without inventing a match.
6. Assemble and provenance-validate the fused artifact.
7. Stream it to an atomic app-private file and only then mark FUSED_LOCAL.

Malformed input is FAILED and retryable. Cancellation does not get translated into success. None of RECEIVED, PROCESSING, PENDING_PAIRING, FUSED_LOCAL or FAILED changes the tracker match's own completed/outbox state.

## Verification

- `:core:test`: 120 tests, all passed.
- `:core:realFusionFixtureTest`: 6 private tests, all passed; no private fixture entered Git.
- `:app:assembleDebug`: passed.
- Debug APK bytes: 69,962,773.
- Debug APK SHA-256: `a313b414870030978027b6ee1c447477e42a03cfcfcb84a9367e453b2e7125d5`.
- Five-hour usage: 52% at checkpoint start; 74% after implementation and gates.

## Remaining R3 boundary

Checkpoint 4 will expose persisted status and retry, cover storage/permission/interruption behavior, build the final isolated test APK and provide a short physical-phone script. Only then should the user be alerted to install and validate. R4 must be discussed explicitly after R3 acceptance; it is not an automatic continuation.
