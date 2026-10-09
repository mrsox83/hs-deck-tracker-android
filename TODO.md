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

No further implementation unit is assigned inside R0-R2. Additional offline hardening should be selected deliberately from the deferred semantics in the final audit rather than expanding into Android integration.

Scheduled continuation: resume at 02:50 Central on 2026-10-09, record fresh usage, and continue evidence-driven R0-R2 hardening. The 02:00-07:00 ceiling is 95% of the five-hour window. If the seven-day limit blocks work, one reset credit is authorized; reread usage afterward and stop by 90% of the reset five-hour window.

Owner: user/project decision.

- [ ] Explicitly authorize a later phase before any R3-R6 Android capture, UI, permissions, persistence, merge or deployment work begins.
