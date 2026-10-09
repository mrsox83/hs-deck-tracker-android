# Mobile fusion sparse-snapshot checkpoint

Date: 2026-10-08. Scope: approved offline R0-R2 hardening only.

## Failure isolated

The real Rafaam artifact was produced and consumed in separate test JVMs, each capped at 512 MiB. The original full-state snapshot artifact measured 139,607,285 bytes. Its fresh consumer still failed with `OutOfMemoryError` during `EvidenceRef` deserialization, so retaining the producer/reducer graph was not the sole cause.

Top-level byte ranges in that artifact were:

- snapshots: 133,497,716 bytes (95.62%);
- entities: 5,684,215 bytes (4.07%);
- events: 400,821 bytes (0.29%);
- choices: 20,875 bytes.

Across 1,144 snapshots, repeated entity-tag state through counters occupied 80,082,092 bytes, and counters through players occupied 44,420,330 bytes. This was the compaction target authorized by the implementation plan's sparse-delta requirement.

## Compatible compaction

`FusionSnapshot.stateMode` distinguishes `FULL` checkpoints from `DELTA` records and defaults to `FULL` when absent, so existing version-1 artifacts retain their prior interpretation. The reducer emits full state for the first snapshot, every turn signal and the last-valid boundary. Action boundaries carry only changed nested entity tags/counters and changed player/quest records.

`FusionSnapshotMaterializer` reconstructs full views lazily. Current reducer maps are monotonic: tags are overwritten rather than removed, counters derive from retained tags, and observed players/quests persist. Removal tombstones are therefore unnecessary for the current R2 state model.

Both the codec validator and materializer reject a stream whose first snapshot is a delta. This prevents a malformed compact artifact from silently reconstructing incomplete state. Empty snapshot lists remain valid for evidence with no snapshot boundaries.

## Measured result

The same exact fixture now produces an 11,033,645-byte artifact, a reduction of 128,573,640 bytes (92.1%). The snapshot section is 4,924,095 bytes; the event, entity and choice byte ranges are unchanged.

The fresh consumer passed under the unchanged 512 MiB limit:

- decode time: 540 ms;
- heap used after decode: 71,857,664 bytes;
- final-state lazy materialization time: 78 ms;
- heap used after final-state materialization: 81,294,848 bytes;
- maximum heap: 536,870,912 bytes;
- decoded snapshots: 1,144;
- decoded events: 917;
- decoded entities: 330.

The recorded heap values and 81,294,848-byte peak memory-pool sum are JVM measurements, not a claim of exact concurrent process RSS.

## Gates

- ordinary `:core:test`: 112 tests, 0 failures, 0 errors, 0 skipped;
- private `:core:realFusionFixtureTest`: 5 tests, 0 failures, 0 errors, 0 skipped;
- isolated `:core:realFusionArtifactLifecycleTest`: 1 test, 0 failures, 0 errors, 0 skipped.

Private fixture content remains outside Git. The four tracker patches remain preserved. No Android integration, merge or deployment was attempted.
