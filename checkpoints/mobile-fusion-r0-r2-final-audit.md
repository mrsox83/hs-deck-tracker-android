# Mobile fusion R0-R2 final audit

Date: 2026-10-08. Branch: `feature/mobile-fusion-offline`.

## Scope preservation

The approved tracker baseline remains commit `d3bc0fb15c6cf4e29ba75fc0aac54b1ee75541bd`. Its four consecutive tracker patches remain in history:

1. `a706bad` — inferred friendly card plays;
2. `e4aad4e` — friendly-play recovery after hand gaps;
3. `51fbec1` — hand OCR feedback-loop prevention;
4. `d3bc0fb` — manual match-export recovery.

The R0-R2 range after that baseline changes only checkpoint/TODO/work-log documentation, `core/build.gradle.kts`, and the dedicated `core` fusion implementation and tests. No Android capture, UI, permissions, persistence, legacy tracker logic, merge or deployment path is changed.

## Compatibility and evidence audit

- `FusionArtifactCodec` is the strict `hs-fused-match/1` boundary: schema presence/type/version and provenance are checked on import and export.
- String and stream encode/decode equality are covered for the synthetic artifact; large artifacts have a direct-to-stream export path.
- Accepted pairing characteristics require exact evidence from both Power and tracker sources.
- Non-unknown typed claims, canonical events, entity histories, populated choice stages and snapshots must resolve to a known source and an exact source line or JSON pointer.
- Duplicate wrappers share the inner Power source identity while retaining physical artifact aliases.
- Unknown raw tags remain retained; unsupported semantics remain unknown rather than inferred.
- Private fixtures stay outside Git and are selected only through the explicit Gradle property.

## Validation gates

The complete ordinary suite passed 111 tests, the private fixture gate passed 3 tests, and the isolated lifecycle gate passed 1 test, with no failures, errors or skips. The real 22,601-event artifact passes provenance validation and direct-to-stream export; its 1,144 snapshots decode successfully in a fresh 512 MiB consumer.

The first isolated artifact measured 139,607,285 bytes and still exhausted a fresh 512 MiB consumer. Profiling showed snapshots consumed 95.62% of the file. Schema-compatible sparse deltas plus full first/turn/last checkpoints reduced the artifact to 11,033,645 bytes; isolated decoding completed in 540 ms with 71,857,664 bytes used after decode. Lazy reconstruction of the final full state took 78 ms and reported 81,294,848 bytes used afterward. Legacy artifacts default to `FULL` snapshot semantics, and lazy materialization restores full snapshot views without eagerly retaining another complete list.

## Deliberately deferred

- Android capture or export integration;
- UI, permissions and persistence changes;
- PC adapters;
- game-build-specific Herald threshold interpretation;
- readiness, attack availability, deck origin and broader generated/transform semantics;
- R3-R6 implementation;
- merge and deployment.
