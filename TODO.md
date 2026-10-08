# Mobile fusion R0–R2

Approved scope: offline mobile evidence fusion only. No merge, deployment, PC adapter, or R3–R6 Android integration.

- [x] Verify shell execution and requested remote branch head.
- [x] Check out `feature/mobile-fusion-offline` at `d3bc0fb15c6cf4e29ba75fc0aac54b1ee75541bd`, preserving all four tracker patches.
- [ ] Preflight: establish GitHub write authentication. CLI authentication fails; an up-to-date push dry run is insufficient proof.
- [x] Preflight: pass `./gradlew :core:test` with downloaded dependencies and writable Gradle/Android caches.
- [x] R0: obtain exporter v0.4 source, catalog/schema inventory, and private mobile fixtures from plan references; record exact hashes and independently verified expectations.
- [x] R0: record installed tracker build/signing/source uncertainty without claiming the branch proves installed APK identity.
- [x] R1: implement versioned evidence schema, adapters, identities, conservative pairing, replay deduplication, and compact output with provenance.
- [x] R1: verify shuffled/repeated import, composite tracker rejection, ambiguous pairs, and partial-result coverage.
- [ ] R2: implement time-scoped entities, block ordering, counters, separate choice stages, turn boundaries, and snapshots with unknown fields retained.
- [ ] R2: assert critical mobile facts and synthetic edge cases; verify every accepted claim traces to exact evidence.
- [ ] Run relevant core/schema/build checks and record reviewable checkpoints; preserve legacy exports and tracker behavior.

Current implementation checkpoint: the pure-JVM schema, adapters, deterministic coordinator, conservative pairing and first Power evidence reducer are implemented with synthetic tests. Real-fixture acceptance and the remaining R1/R2 assertions are still open.

Real-fixture checkpoint: the exact Rafaam tracker/exporter pair now passes a dedicated private-fixture gate, including wrapper/inner hashes, event and choice counts, separate Raze/Enthrall stages, action ordering, target-zero semantics, clock normalization and conservative pairing. Broader fixture coverage and remaining reducer semantics are still open.

Implementation is blocked at preflight. Do not mark fixture acceptance complete from the plan's narrative alone.
