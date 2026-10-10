# Mobile fusion R0–R2

Approved scope: offline mobile evidence fusion only. No merge, deployment, PC adapter, or R3–R6 Android integration.

- [x] Verify shell execution and requested remote branch head.
- [x] Check out `feature/mobile-fusion-offline` at `d3bc0fb15c6cf4e29ba75fc0aac54b1ee75541bd`, preserving all four tracker patches.
- [x] Preflight: establish GitHub write authentication. Real pushes through `239ee23` succeeded and the remote branch tip was independently verified.
- [x] Preflight: pass `./gradlew :core:test` with downloaded dependencies and writable Gradle/Android caches.
- [x] R0: obtain exporter v0.4 source, catalog/schema inventory, and private mobile fixtures from plan references; record exact hashes and independently verified expectations.
- [x] R0: record installed tracker build/signing/source uncertainty without claiming the branch proves installed APK identity.
- [x] R1: implement versioned evidence schema, adapters, identities, conservative pairing, replay deduplication, and compact output with provenance.
- [x] R1: verify shuffled/repeated import, composite tracker rejection, ambiguous pairs, and partial-result coverage.
- [x] R2: implement time-scoped entities, block ordering, counters, separate choice stages, turn boundaries, and snapshots with unknown fields retained.
- [x] R2: assert critical mobile facts and synthetic edge cases; verify every accepted claim traces to exact evidence.
- [x] Run relevant core/schema/build checks and record reviewable checkpoints; preserve legacy exports and tracker behavior.

Current implementation checkpoint: the approved offline R0-R2 core is implemented and locally verified. Every accepted pairing characteristic and non-unknown typed claim is required to resolve to a known fused source and an exact source line or tracker JSON pointer; entity histories, canonical events, choice stages and snapshots are checked too.

Reconnect/session-rollover checkpoint: conservative two-session continuation is implemented for the HSC-004/HSC-009 evidence while remaining inside offline R0-R2. A stitch requires bounded clocks, the tracker endpoint, an incomplete non-truncated earlier source, an evidenced completed later source, a uniquely resumed open choice, both player/controller identities, stable entity identities and raw-turn continuity. The fused artifact retains both original sources, exact line/JSON-pointer evidence, parent lineage and an explicit gap diagnostic. Weak candidates remain unstitched.

Real-fixture checkpoint: the exact Rafaam tracker/exporter pair passes deep assertions, including wrapper/inner hashes, event and choice counts, separate Raze/Enthrall stages, action ordering, target-zero semantics, clock normalization and conservative pairing. The private corpus gate also pins and parses all 32 current tracker records and reduces all 48 source matches across 21 current exporter bundles, covering 21,018 canonical events and 29,674 compact snapshots. Unsupported higher-level game semantics remain deliberately unknown.

Remote write authentication is proven for `feature/mobile-fusion-offline`. The isolated lifecycle and sparse-snapshot checkpoint supersedes the prior pause handoff. No merge or deployment was attempted.

## Assigned for the next Codex window

Owner: next Codex session on `feature/mobile-fusion-offline`, after refreshing usage and confirming the branch tip.

- [x] R0-R2 hardening: measure full real-artifact stream decoding in an isolated consumer lifecycle that does not retain the reducer/source graph; record output size, memory conditions and pass/fail under an explicit 512 MiB heap.
- [x] Characterize dominant artifact sections and implement schema-compatible sparse snapshot deltas with full checkpoints, preserving provenance and defaulting legacy artifacts to full-state semantics.
- [x] Re-run `:core:test`, `:core:realFusionFixtureTest`, and the isolated lifecycle gate; record the results and push the completed checkpoint.

The exact Rafaam artifact now encodes to 11,033,664 bytes instead of 139,607,285 bytes. A fresh isolated consumer decoded all 1,144 snapshots in 532 ms with 73,551,360 bytes used after decode under a 536,870,912-byte maximum heap; lazy reconstruction of the final full state took 70 ms and reported 82,988,544 bytes used afterward. The ordinary suite passed 114 tests, the expanded private fixture gate passed 6 tests, and the isolated consumer gate passed 1 test, all without failures, errors or skips. The private tracker inventory now pins 32 records.

No further **new** R0-R2 implementation unit is assigned. The in-flight reconnect unit has reached its tested, documented closeout boundary. R0-R2 is now closed to further discretionary hardening. The user authorized R3 with a separate test/debug app identity so the working tracker installation and data remain untouched. Stop for user participation only when the physical-device validation package and script are ready. HSC-009 Android journal work belongs to R3; R4-R6 remain separate phase decisions and must be surfaced rather than entered through an open-ended hardening loop.

## R2 closeout and R3 authorization gate — owner direction, 2026-10-09

This section supersedes the earlier open-ended instruction to "continue evidence-driven R0-R2 hardening." **Do not abruptly halt work already in progress.** Finish and verify that bounded unit first. No recurring polish loop and no automatic phase transition.

Owner: completed by the active R2 agent; R3 subsequently authorized by the user.

- [x] **Existing in-flight R2 task only:** inspected the branch, local changes, task direction and active checkpoint; identified the reconnect/session-rollover acceptance criteria.
- [x] Finished that bounded unit without starting unrelated R2 enhancements; preserved the four tracker patches, raw-fixture privacy, schema and provenance rules.
- [x] Passed `:core:test`, the complete real-fixture gate and the isolated lifecycle gate after the final changes.
- [x] Reviewed and documented the accepted results and limitations in WORK_LOG and `checkpoints/mobile-fusion-session-continuation.md`.
- [x] **Transition review:** reported the closeout and specific R3 proposal; the user explicitly authorized R3 with a separate test/debug identity.

Stop rule: after safe R2 closeout, **do not initiate further optional/offline hardening** merely to use available time or tokens. Only a concrete, reproducible defect that threatens R2 acceptance may trigger another bounded R2 fix; log its evidence and acceptance criterion. Defer unsupported gameplay semantics, speculative enhancements and QoL ideas to their proper project backlog. If uncertainty is material, ask the user to prioritize rather than invent another task.

Coordination: GitHub and Drive working copies do not update each other automatically. Refresh the remote branch and this TODO before starting/resuming work; reconcile any newer agent changes without overwriting them. This TODO update is a direction for the next safe checkpoint, not a live cancellation or claim that the active agent has read it.

Usage/schedule guard: the prior 02:50 Central 2026-10-09 continuation may be used to finish **the already-started bounded R2 task only**, not to open-endedly polish R2. Retain the previously authorized 02:00–07:00 five-hour ceiling of 95% and the one conditional seven-day reset credit (reread usage afterward; 90% ceiling). Respect hard usage ceilings; if reached, checkpoint recoverably and resume the unfinished unit instead of abruptly abandoning it.

Owner: current Codex session on `feature/mobile-fusion-offline`.

- [x] R3 authorized with a separate test/debug app identity; preserve the working installation and its data.
- [x] R3 checkpoint 1: confirm `com.stroexd.hsdecktracker.debug` isolation; journal completed matches before persistence/export; add an idempotent durable outbox and automated interruption/retry gates.
- [x] R3 checkpoint 2: persist and recover an active match draft without duplicating a completed match.
- [x] R3 checkpoint 3: import selected exporter bundles post-match and persist fusion/outbox status separately from tracker match status.
- [x] R3 checkpoint 4: expose one visible retry/status flow and complete automated storage/permission/interruption gates.
- [x] Alert the user when the R3 test APK and physical-device validation script are ready.
- [ ] Stop and explicitly surface readiness before beginning R4, R5 or R6.

R3 source/build work is complete at the isolated-phone-validation boundary. Physical device acceptance remains pending and is not implied by the passing local gates. Do not begin R4 until the user reports the R3 phone results and explicitly authorizes the transition.

### R3 device identity correction — 2026-10-09

- [x] Treat the first phone gate as failed safely before installation: Android offered **Update**, and the user cancelled.
- [x] Supersede the `.debug` delivery for this phone without deleting its historical checkpoint.
- [x] Add and independently inspect a dedicated `fusionTest` build identity: package `com.stroexd.hsdecktracker.fusiontest`, version `1.5.0-fusiontest`, label **HS Deck Tracker Fusion Test**.
- [x] Build and stage a corrected v2 APK and phone-validation checklist.
- [x] Device isolation gate: v2 installed as a new app and opened with no historical decks or games from the working tracker; the original tracker retained its active decks and match history.
- [x] Force-stop recovery gate: user played turn 1, stopped during the opponent turn, relaunched and continued; the tracker produced one completed six-turn/6:27 record whose original start duration survived, while the exporter ZIP preserves one incomplete pre-stop fragment and one completed post-relaunch fragment.
- [x] Confirm the completed record/fusion result survives force-stop/reopen without returning to the interrupted active draft.
- [ ] Characterize the missing Turn 2 timeline row. Current evidence indicates a bounded OCR/capture gap: the UI emits only turn groups containing timeline events, while duration and pre-stop Start events persisted.
- [x] Complete phone evidence-import cancellation, successful local fusion and persisted-status checks. Cancellation correctly claims no success; the selected two-fragment ZIP reports **Fused artifact saved locally** and remains saved after reopen.
- [x] Complete R3 physical-device acceptance with the v2 APK. Deterministic automated gates cover permission denial, unreadable input and retry; no additional manufactured phone failure is required.

The `.debug` package was not isolated from this phone's existing tracker installation, regardless of the source repository's production-ID assumption. Only the v2 `.fusiontest` package is active for device validation. R3 device acceptance is complete. Stop and explicitly surface R4 scope/prerequisites; do not begin R4 without user authorization.

### R4 authorized scope — 2026-10-09

The user explicitly authorized R4 after accepting the bounded staged plan below. R4 device builds now use the fresh `com.stroexd.hsdecktracker.fusiontestr4` identity because the accepted R3 debug signing key is unavailable; this preserves both the working tracker and the installed R3 Fusion Test data. Hearthstone locks the user's phone to landscape while open, so do not require an artificial portrait/landscape rotation test during gameplay. Retain dimension/inset-safe coordinate handling and test landscape startup, lock/unlock, capture stop/reconnect, and any fold or window-size change that can occur without leaving landscape.

R4 capture decisions from the owner:

- Reuse and audit the tracker's existing screen-share/OCR module and frame-source lifecycle; do not build a parallel capture stack.
- Give the user an explicit, clearly explained privacy-versus-capability choice among the existing supported capture paths. Prefer Hearthstone-only single-app capture when available, but allow the user to opt into the more compatible/broader existing screen-share path after warning that non-Hearthstone pixels such as notifications may be processed. Visual capture remains opt-in and timestamp-only operation remains available.
- Retain only explicit trigger/manual-bookmark keyframes or crops needed for evidence; do not persist an unrestricted screen recording or unnecessary full-display frames.
- Do not implement a rolling pre-event frame buffer.
- Timestamp bookmarks must continue working when visual capture is unavailable or stopped.
- Do not use an exact battery-percentage threshold as an acceptance gate. The owner will monitor practical battery impact and request reduced demand if needed; still reject obvious thermal escalation, gameplay stutter or runaway capture/storage.

Implementation sequence:

- [x] **R4.1 source/build:** audited and extended the existing screen-share/OCR pipeline; added the explicit privacy/capability selector and disclosure, normalized regions, bounded trigger/manual-bookmark keyframes, lifecycle cleanup, and timestamp-only bookmark fallback. No second capture engine or rolling buffer was added.
- [ ] **R4.1 phone acceptance:** install the new Fusion Test APK and verify privacy-first Hearthstone selection, informed compatibility mode, timestamp-only mode, manual bookmarks, landscape startup, lock/unlock, capture stop/reconnect, and bounded evidence behavior. Do not begin R4.2 until this result is reviewed.
  - [x] Privacy-first single-app capture becomes available after explicitly selecting Privacy First; the revised always-visible choice UI is preferred over the original dropdown.
  - [x] Deck-tracking overlay persists during the phone test.
  - [x] Source/build fix for the narrow-overlay header: Close remains the final pinned action; Bookmark and Open App moved into a three-dot overflow without shrinking touch targets. Fusion R4 Test APK rebuilt.
  - [x] Phone-verify the v4 narrow-overlay package: user reports all requested checks passed; Minimize, More actions and Close visible; overflow exposes Bookmark/Open App; Bookmark works; Close dismisses normally.
  - [ ] Review remaining original R4.1 acceptance evidence: timestamp-only/compatibility behavior, lock/unlock, capture stop/reconnect and bounded retained evidence. The v4 layout-only pass does not independently verify these checks.
- [ ] **R4.2:** enable three pilot region groups in diagnostic/shadow mode: hero state (health/armor, portrait, hero power and weapon); turn/resources (active player, mana and hand count when reliable); event recovery (card-play area, history rail and reconnect/resume cues).
- [ ] **R4.3:** validate each observation independently and promote fields individually through `experimental` -> `corroborating` -> `accepted`; ambiguous values remain unknown/inferred with confidence and evidence. No experimental recognizer may create an authoritative match event.
- [ ] **R4.4:** close R4 after the pilot groups provide trustworthy bounded enrichment. Defer detailed board/minion state, secrets, deck counts and choice-panel recognition unless real missing-data evidence justifies a separately accepted addition.
- [ ] After R4 source and device behavior stabilize—and before any public-release, merge/deployment or broader-distribution planning—remind the owner to consider an optional Codex Security review of MediaProjection boundaries, Android permissions/exported components, URI handling, retained keyframes and cleanup. Do not install or run it without separate authorization, and do not make it a blocker for ordinary R4 work.
- [ ] Alert the user when physical-device validation is ready and again before moving to R5.

Restart point: live five-hour usage was already 79% when authorization was received, above the owner's previously stated 60% ceiling. No R4 source implementation began in that window. Resume with R4.1 only after rereading the active usage window and the project protocol/plan.
