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

Real-fixture checkpoint: the exact Rafaam tracker/exporter pair passes deep assertions, including wrapper/inner hashes, event and choice counts, separate Raze/Enthrall stages, action ordering, target-zero semantics, clock normalization and conservative pairing. The private corpus gate also pins and parses all 29 current tracker records and reduces all 48 source matches across 21 current exporter bundles, covering 21,018 canonical events and 29,674 compact snapshots. Unsupported higher-level game semantics remain deliberately unknown.

Remote write authentication is proven for `feature/mobile-fusion-offline`. The isolated lifecycle and sparse-snapshot checkpoint supersedes the prior pause handoff. No merge or deployment was attempted.

## Assigned for the next Codex window

Owner: next Codex session on `feature/mobile-fusion-offline`, after refreshing usage and confirming the branch tip.

- [x] R0-R2 hardening: measure full real-artifact stream decoding in an isolated consumer lifecycle that does not retain the reducer/source graph; record output size, memory conditions and pass/fail under an explicit 512 MiB heap.
- [x] Characterize dominant artifact sections and implement schema-compatible sparse snapshot deltas with full checkpoints, preserving provenance and defaulting legacy artifacts to full-state semantics.
- [x] Re-run `:core:test`, `:core:realFusionFixtureTest`, and the isolated lifecycle gate; record the results and push the completed checkpoint.

The exact Rafaam artifact now encodes to 11,033,645 bytes instead of 139,607,285 bytes. A fresh isolated consumer decoded all 1,144 snapshots in 540 ms with 71,857,664 bytes used after decode under a 536,870,912-byte maximum heap; lazy reconstruction of the final full state took 78 ms and reported 81,294,848 bytes used afterward. The ordinary suite passed 112 tests, the expanded private fixture gate passed 5 tests, and the isolated consumer gate passed 1 test, all without failures, errors or skips.

No further **new** R0-R2 implementation unit is assigned. The completed baseline, final audit and passing gates remain valid as of their recorded commit. An in-flight R2 unit is not silently cancelled by this status.

## R2 closeout and R3 authorization gate — owner direction, 2026-10-09

This section supersedes the earlier open-ended instruction to "continue evidence-driven R0-R2 hardening." **Do not abruptly halt work already in progress.** Finish and verify that bounded unit first. No recurring polish loop and no automatic phase transition.

Owner: currently active R2 agent, then user for R3 decision.

- [ ] **Existing in-flight R2 task only:** at the next safe checkpoint, inspect the current branch/head, local uncommitted changes, agent task description, TODO, WORK_LOG and checkpoints. Identify the exact started unit and its acceptance criteria. Do not assume previously green tests cover changes since the last checkpoint.
- [ ] Finish that unit to a coherent boundary, including any immediately necessary bug fixes or regressions introduced by the work. Preserve the four tracker patches, raw-fixture privacy, versioned schema and evidence/provenance rules. Do not start unrelated R2 enhancements while closing this work.
- [ ] Run the tests appropriate to the touched code, including `:core:test` and the real-fixture/lifecycle gates when relevant; review failures and correct reproducible in-scope defects. Explicitly check compatibility and any changed artifact/stream behavior. Never mark DONE solely because implementation was written or tests previously passed.
- [ ] Review the diff for unintended changes, document accepted results and remaining limitations in WORK_LOG and a checkpoint, update this task's status and exact branch/commit, and safely commit/push the verified checkpoint. If the task cannot be completed due to a real blocker, time/usage ceiling or failing gate, retain recoverable work, record the specific blocker and next repair step, leave the checkbox OPEN, and resume that same task before any R3 start. Do not mislabel an unverified or WIP commit as release-ready.
- [ ] **Transition review:** once the in-flight unit is closed with its acceptance evidence (or explicitly reported blocked), present a concise R2 closeout and specific R3 proposal to the user. Ask for explicit R3 authorization; do not commence it on the agent's own initiative.

Stop rule: after safe R2 closeout, **do not initiate further optional/offline hardening** merely to use available time or tokens. Only a concrete, reproducible defect that threatens R2 acceptance may trigger another bounded R2 fix; log its evidence and acceptance criterion. Defer unsupported gameplay semantics, speculative enhancements and QoL ideas to their proper project backlog. If uncertainty is material, ask the user to prioritize rather than invent another task.

Coordination: GitHub and Drive working copies do not update each other automatically. Refresh the remote branch and this TODO before starting/resuming work; reconcile any newer agent changes without overwriting them. This TODO update is a direction for the next safe checkpoint, not a live cancellation or claim that the active agent has read it.

Usage/schedule guard: the prior 02:50 Central 2026-10-09 continuation may be used to finish **the already-started bounded R2 task only**, not to open-endedly polish R2. Retain the previously authorized 02:00–07:00 five-hour ceiling of 95% and the one conditional seven-day reset credit (reread usage afterward; 90% ceiling). Respect hard usage ceilings; if reached, checkpoint recoverably and resume the unfinished unit instead of abruptly abandoning it.

Owner: user/project decision.

- [ ] Explicitly authorize R3 before any R3-R6 Android capture, UI, permissions, persistence, merge or deployment work begins.
