# HSC-009/010 user-note projection: source-local verification

Owner approved bounded implementation, tests and independent review at 16:36 UTC. Base: `46b39b420b7c141df32f01a1071ad057e1d357f3`, branch `feature/mobile-fusion-offline`. SOURCE-LOCAL PASS only: no commit, push, APK, release or phone acceptance. Broad HSC issues remain OPEN, PRIORITY: UNASSESSED. Original incident cause is not newly established.

## Behavior and compatibility

Newly assembled fusions can carry optional typed `userNote`, explicitly user-authored annotation rather than gameplay observation or decision-time knowledge. `ABSENT`, `NULL` and `TEXT` retain raw input semantics; TEXT preserves empty, whitespace, Unicode and newlines exactly. Invalid non-string notes are rejected rather than silently coerced. Annotation includes tracker match identity, record revision, SHA256 and `/notes` evidence with `tracker-user-note/1` rule. Missing ingestion witness yields no annotation, not an invented empty note.

The adapter binds the annotation to its decoded ingestion snapshot. Assembly rejects stale/modified records, changed source hashes/types/aliases or invalid revisions; changed records must be ingested again. This does not search history or choose an unavailable latest revision. Android/offline producer identities advance to `android-r3/9` and `offline-r2/9`; schema remains `hs-fused-match/1`. Optional field defaults preserve old JSON compatibility; no migration or backfill. Frozen /8 artifacts/APKs retain their original identities.

Serialized validation checks one linked tracker source, source ID/hash/alias association, positive revision, timeline source, exact pointer/rule and state/text consistency. Without original tracker bytes it cannot authenticate arbitrary claimed text or a positive revision; hashes are provenance references, not signatures. Pairing safeguards and source selection are unchanged.

Existing FUSED_LOCAL import/reexport returns the existing artifact. A later tracker note does not revise its fused bytes. Regression exercises repository restart, import of a newer edited record, reexport byte equality and rejection of a conflicting write under the old logical identity. Later-note annotation/revision workflow remains separately unimplemented.

## Validation and review

- Final `:core:test --offline --no-daemon` using existing user dependency cache and isolated ignored output: **341 tests, 54 suites, 0 failures/errors/skips**, executed PASS in 49 seconds. Includes **8 UserNoteProjectionTest tests** and existing lifecycle/pairing regressions. Kotlin/Java test compilation passed. No APK packaging, signing or Android runtime test.
- Earlier focused six-test iteration passed in 1 minute 23 seconds; final eight-test coverage is included in the aggregate run above. No failing-before result claimed for this additive feature.
- Read-only independent reviewer `note_projection_review`: no blocking defects. Lifecycle coverage gap was addressed with actual repository/import/reexport coverage; recommended producer /9 bump applied and final delta re-reviewed without blockers.
- `git diff --check -- core`: PASS. Existing global Gradle deprecation warnings remain; no unrelated configuration changes.
- First default-output attempt failed on locked old Kotlin intermediates. Isolated output resolved that problem without deleting files or stopping unrelated processes. A project-local-cache attempt lacked Android Gradle plugin resolution; existing user cache supported the final offline run. Initial wrapper invocation populated its configured Gradle distribution cache. No project dependencies or machine configuration changed.
- Detailed local logs/XML: ignored `.gradle/user-note-20261026-focused.log`, `.gradle/user-note-20261026-aggregate.log`, `.gradle/user-note-20261026/output/core/test-results/test/`. Raw gameplay/private sources were not used or modified.

## Reviewed source hashes (SHA256)

| File under core/src | SHA256 |
|---|---|
| main/kotlin/com/stroexd/hsdecktracker/core/fusion/EvidenceAdapters.kt | cb854caee8bcefe57d7800f77d276a021d64c2076fa633690f0e50f528a856c1 |
| main/kotlin/com/stroexd/hsdecktracker/core/fusion/FusionCoordinator.kt | b7069387082c7c88b1a6dbe4d46c28f6da1ac9525bcb0e30b4df8e456e58cc0d |
| main/kotlin/com/stroexd/hsdecktracker/core/fusion/FusionModel.kt | 9fd70c255c1e2a4c3b60c198726d9b501d764e938ab88c8e774ee05087cc5949 |
| main/kotlin/com/stroexd/hsdecktracker/core/fusion/FusionProvenanceValidator.kt | e57b441699e25885d034bdb0222cd0a56c448d2c3f39f1c8c490a3ea2055d0bc |
| test/kotlin/com/stroexd/hsdecktracker/core/fusion/FusionCoreTest.kt | 93fbbcf964e95ac9489a6396ef8acc2783fb40c6c28510a3e41f16d1061780a6 |
| test/kotlin/com/stroexd/hsdecktracker/core/fusion/UserNoteProjectionTest.kt | 6b82701e3b05c50a17fb3c112eb1436b2e44aa8263fd7a30b1766eb8b4bb9af1 |

## Next decision and shared status

No active implementation/reviewer remains. Next recommendation: approve source-only publication of these six source/test files and this sanitized checkpoint, then guarded Resource Exchange/canonical reconciliation as source-only, FIRST DELIVERED none. APK preparation/delivery and phone acceptance remain separate decisions. No additional game required now.

Pending shared reconciliation under HSC-009/010: prior shared wording says fused-note projection unimplemented; new local evidence establishes reviewed/tested implementation for NEW fusions only. Resource Exchange/canonical sources are available, but current authorization forbids publication, so no external write attempted. This checkpoint is not a canonical update or broad issue closure. Preserve all unrelated dirty instructions/docs/helpers/private evidence and frozen artifacts.
