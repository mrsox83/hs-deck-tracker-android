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

Real-fixture checkpoint: the exact Rafaam tracker/exporter pair now passes a dedicated private-fixture gate, including wrapper/inner hashes, event and choice counts, separate Raze/Enthrall stages, action ordering, target-zero semantics, clock normalization and conservative pairing. Broader fixture coverage and remaining reducer semantics are still open.

Remote write authentication is proven for `feature/mobile-fusion-offline`; `239ee23482217e2b068ea2a06658f1c2c1dfc236` is the last implementation checkpoint, followed only by the pause-handoff documentation commit. No merge or deployment was attempted.

## Assigned for the next Codex window

Owner: next Codex session on `feature/mobile-fusion-offline`, after refreshing usage and confirming the branch tip.

- [ ] R0-R2 hardening: measure full real-artifact stream decoding in an isolated consumer lifecycle that does not retain the reducer/source graph; record output size, peak-memory conditions and pass/fail without increasing heap merely to hide the result.
- [ ] If isolated decoding is still memory-heavy, characterize which artifact sections dominate size before proposing any schema-compatible compaction. Preserve provenance and do not implement Android integration as part of that measurement.
- [ ] Re-run `:core:test` and `:core:realFusionFixtureTest`, then commit and push any completed checkpoint.

Owner: user/project decision.

- [ ] Explicitly authorize a later phase before any R3-R6 Android capture, UI, permissions, persistence, merge or deployment work begins.
