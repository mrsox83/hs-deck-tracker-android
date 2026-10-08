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

- `hs-fused-match/1` is explicitly serialized with defaults and now has an encode/decode equality test.
- The decoded artifact is rechecked by `FusionProvenanceValidator`.
- Accepted pairing characteristics require exact evidence from both Power and tracker sources.
- Non-unknown typed claims, canonical events, entity histories, populated choice stages and snapshots must resolve to a known source and an exact source line or JSON pointer.
- Duplicate wrappers share the inner Power source identity while retaining physical artifact aliases.
- Unknown raw tags remain retained; unsupported semantics remain unknown rather than inferred.
- Private fixtures stay outside Git and are selected only through the explicit Gradle property.

## Validation gates

The complete ordinary suite passed 108 tests and the private fixture gate passed 3 tests, with no failures, errors or skips. The ordinary total increased by one for the versioned-artifact round-trip assertion, so this checkpoint is accepted locally.

## Deliberately deferred

- Android capture or export integration;
- UI, permissions and persistence changes;
- PC adapters;
- game-build-specific Herald threshold interpretation;
- readiness, attack availability, deck origin and broader generated/transform semantics;
- R3-R6 implementation;
- merge and deployment.
