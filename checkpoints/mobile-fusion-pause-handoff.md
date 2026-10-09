# Mobile fusion pause handoff

Date: 2026-10-08. Status: superseded by the sparse-snapshot lifecycle checkpoint.

## Repository state

- Repository: `mrsox83/hs-deck-tracker-android`
- Branch: `feature/mobile-fusion-offline`
- Verified local and remote tip before this handoff update: `239ee23482217e2b068ea2a06658f1c2c1dfc236`
- Tracker baseline: `d3bc0fb15c6cf4e29ba75fc0aac54b1ee75541bd`
- The four tracker patches remain preserved.
- No merge or deployment has been performed.

This handoff document, TODO correction and final log entry must be committed and pushed together. After that push, use the newer handoff commit as the authoritative branch tip.

## Last verified gates

- Ordinary `:core:test`: 112 tests, 0 failures, 0 errors, 0 skipped.
- Private `:core:realFusionFixtureTest`: 3 tests, 0 failures, 0 errors, 0 skipped.
- Private fixture directory: `H:\My Drive\HSReplay`
- No private fixture content is stored in Git.

The local Windows test runtime used:

- JDK 17: `C:\Users\Xs_da\AppData\Local\Temp\temurin17\jdk-17.0.20.1+1`
- Gradle executable: `.gradle\wrapper\dists\gradle-8.14.3-bin\cv11ve7ro1n3o1j4so8xd9n66\gradle-8.14.3\bin\gradle.bat`
- Gradle cache: `C:\Users\Xs_da\.gradle`

Google Drive may lock `core\build` after a Gradle invocation. Verify the exact resolved path and move only that directory to a new uniquely named location under `C:\Users\Xs_da\AppData\Local\Temp` before the next gate. Do not delete or broadly move workspace paths.

## Proven state

- Offline R0-R2 schema, adapters, conservative pairing, reducer, provenance validation and strict artifact codec are implemented.
- String and stream round trips pass on the synthetic artifact.
- The real 22,601-event Rafaam artifact validates and streams completely to disk under the default test heap.
- Schema presence/type/version and provenance are enforced at the codec boundary.
- The feature branch has been pushed successfully; write authentication is proven despite the earlier invalid `gh` token/API status.

## Completed lifecycle unit

The full real artifact initially remained too large for a fresh 512 MiB consumer even without the reducer/source graph: the 139,607,285-byte artifact exhausted the heap during `EvidenceRef` deserialization. Section profiling showed that snapshots occupied 133,497,716 bytes (95.62%), almost entirely from repeated full entity and counter state.

R2 now stores sparse deltas at action boundaries and full checkpoints at the first snapshot, turn signals and last-valid boundary. Legacy artifacts remain compatible because a missing `stateMode` defaults to `FULL`. `FusionSnapshotMaterializer` lazily reconstructs full state for consumers.

The resulting artifact is 11,033,645 bytes. A fresh isolated 512 MiB consumer decoded it in 540 ms, reporting 71,857,664 bytes used after decode; lazy reconstruction of the final full state took another 78 ms. See `checkpoints/mobile-fusion-sparse-snapshots.md` for exact measurements and validation.

## Boundaries requiring user direction

Do not begin R3-R6 Android integration, capture, UI, permissions or persistence work without explicit user authorization. Do not merge or deploy. Do not consume a usage-reset credit without explicit confirmation.
