# R3 checkpoint 1 — completed-match journal and export outbox

Date: 2026-10-09. Branch: `feature/mobile-fusion-offline`.

## Goal

Eliminate the process-death gap between completed-match persistence and optional SAF export while keeping the user's working tracker installation and data untouched.

## Isolation

The existing debug variant is the approved test channel:

- application ID: `com.stroexd.hsdecktracker.debug`;
- version: `1.5.0-debug`;
- debug signing;
- package-scoped file-provider authority and app-private storage.

It is distinct from the production application ID `com.stroexd.hsdecktracker`. No production package/signing or data migration change was made.

## Durable sequence

`CompletedMatchCommitter` serializes completion and recovery:

1. atomically stage the complete `MatchRecord` in `match-outbox.json`;
2. add the match idempotently to `matches.json`;
3. mark LOCALLY_COMMITTED;
4. when transfer is enabled, mark TRANSFER_PENDING before SAF work;
5. mark LOCALLY_EXPORTED only after WRITTEN or ALREADY_PRESENT;
6. retain TRANSFER_FAILED, attempt count and error text on an ordinary failure.

Cancellation leaves TRANSFER_PENDING so restart recovery can retry. An outbox match ID cannot silently acquire different content. Existing completed matches can enter the journal through manual or bulk recovery without duplication.

REMOTELY_VERIFIED is modeled separately but is never asserted by the local folder exporter; local export does not prove Drive sync.

## Recovery coverage

Pure-JVM tests recreate repositories from disk at each simulated restart and cover:

- staged before `matches.json` commit;
- match present while the outbox is still pending;
- restart during TRANSFER_PENDING;
- unavailable folder/transfer failure;
- later successful retry;
- repeated recovery without another export or match duplicate.

## Gates

- `:core:test`: 116 tests, 0 failures, 0 errors, 0 skipped;
- `:app:compileDebugKotlin`: passed;
- `:app:assembleDebug`: passed;
- APK size: 68,778,914 bytes;
- APK SHA-256: `77bce0caed7e2046178749f4eb402f6fd7e0cbd87d768f737d5615eeaf705537`;
- packaged application ID: `com.stroexd.hsdecktracker.debug`;
- phone installation/lifecycle behavior: not yet tested.

## Next bounded R3 units

1. Active-match draft journaling and recovery.
2. Post-match exporter-bundle import plus fused-artifact persistence/outbox state.
3. One visible recovery/status flow and automated permission/storage interruption gates.
4. Build the test APK and alert the user with a short physical-device script.

R4-R6 remain out of scope until their readiness is explicitly surfaced.
