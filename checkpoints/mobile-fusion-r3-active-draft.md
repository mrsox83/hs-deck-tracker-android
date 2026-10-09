# R3 checkpoint 2 — active-match draft recovery

Date: 2026-10-09  
Branch: `feature/mobile-fusion-offline`  
Scope: R3 persistence only; no R4-R6, merge or deployment

## Acceptance boundary

- A running tracker state has one stable `draftId` across deck recognition and ordinary mutations.
- `active-match.json` durably stores the latest active state through the existing atomic JSON store.
- Startup restores a nonterminal draft before accepting later tracker changes.
- Completion uses `draftId` as the `MatchRecord.id`, stages the completed outbox first, persists the match idempotently, then clears only that draft.
- A draft whose ID is already in the match repository or completed outbox is discarded at startup instead of restored.
- The production app identity and data remain untouched; all eventual phone work uses `com.stroexd.hsdecktracker.debug`.

## Ordering model

Tracker transitions synchronously enqueue `Save`, `Clear` or `Commit` commands into one application-owned channel. A single consumer performs disk changes in order. The post-game tracker reset is marked inactive, so it cannot replace the evidence-bearing draft before `Commit` has passed the durable completed-match boundary. Journal-command failures are logged without terminating the consumer; failed completion never clears the active draft.

## Verification

- `:core:test`: 118 tests, all passed.
- `:app:assembleDebug`: passed.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- APK bytes: 68,959,268
- APK SHA-256: `75088765ccf63f76661b8e60d6e2a919288358919ab7933347d8a7b9e595c5a7`

## Deferred, by design

- Physical-device process-death and relaunch validation.
- Post-match exporter-bundle selection/import and fusion status persistence (R3 checkpoint 3).
- Visible retry/status UI, permission/interruption matrix and phone test script (R3 checkpoint 4).
- R4 capture/bookmark work and all later phases.

The phone-validation alert is intentionally deferred until checkpoints 3 and 4 produce a complete isolated test package and script.
