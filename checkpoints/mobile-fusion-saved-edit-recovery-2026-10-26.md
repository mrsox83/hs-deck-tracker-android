# Saved-edit recovery - bounded source-local validation

**SOURCE VALIDATION PASS / INDEPENDENT REVIEW COMPLETE.** Failing-before reproduced,12 focused and326 ordinary tests pass on the exact reviewed source. Owner approved source-only publication October26; this checkpoint freezes validation evidence, with final commit/remote/shared receipt recorded separately. No APK or phone acceptance is implied.

HSC-009/010, PRIORITY: UNASSESSED. Owner approved focused reproduce/fix/tests/independent review at04:20 UTC October26 and source-only publication/shared reconciliation at04:35 UTC. Existing mrsox83/hs-deck-tracker-android, feature/mobile-fusion-offline base b03a101586960336dce2b1b239504e533ed4bd02. Source-only preflight independently verified live remote equals this base and final source/test hashes below remain unchanged. No APK/package/signing, phone install, merge/deployment, fused-note projection, private evidence or coaching/statistics changes. All unrelated dirty/untracked instructions/docs/reports/four helpers preserved outside publication. Owner withdrew five-hour checking; separate weekly90% ceiling retained. No reset used.

## Failing-before evidence

Added synthetic CompletedMatchRecoveryTest.recoveryPreservesUiSavedRevisionWithoutRestagingOutbox before changing production. It commits R1 with transfer disabled, updates only MatchRepository to R2 through the same persistence path as MatchDetailScreen Save, verifies outbox still R1, reconstructs repositories and invokes startup recovery. It expects saved R2/LOSS/nonempty note. Unchanged production instead persists R1/WIN/empty note.

Offline Gradle8.14.3/JDK21 focused run FAILED in53s:1 test/1 assertion failure,0 errors/skips. Production compile came from unchanged cache; test compilation and execution ran. Original failing XML is .gradle/saved-edit-rollback-20261026/before/core/test-results/test/TEST-com.stroexd.hsdecktracker.core.CompletedMatchRecoveryTest.xml; expected-versus-actual message records the exact revision/result/note rollback. Console receipt outside repo: ../.local-validation/saved-edit-rollback-20261026/before.log. Failing outputs preserved, not overwritten by later gates. No private gameplay fixture involved.

## Bounded change

Only production file: core/src/main/kotlin/com/stroexd/hsdecktracker/core/data/Repositories.kt. New synthetic suite: core/src/test/kotlin/com/stroexd/hsdecktracker/core/CompletedMatchRecoveryTest.kt. No app/UI/schema/dependency/build-input changes.

- Reconcile caller, saved history and outbox by highest record revision. Equal highest revisions must have identical payloads; conflict throws without arbitrarily replacing either payload. Conflicts in obsolete lower revisions do not override unambiguous newer saved truth.
- History update/upsert decisions are protected by the existing JsonFileStore lock; stale writes cannot downgrade newer saved edits. The reconciliation path checks history again atomically after durable outbox staging, retaining stage-before-history recovery semantics.
- Outbox staging is monotonic: stale retries retain newer content/state/attempt metadata. A newer deliberate empty note wins; no 'nonempty always wins' rule. Commit/export and startup recovery use the reconciled record, preserving notes and other edited fields/resultCorrections.
- No automatic export of every UI edit was introduced. Existing export settings, retry stages, match IDs and versioned filenames remain in use. Existing immutable fused artifacts are untouched; adding notes to fused JSON is a separate unimplemented capability.

## Verification

| Gate | Status | Evidence / limits |
|---|---|---|
| Exact UI-path failing-before regression | FAIL expected |1/1 fails on unchanged production; expected R2, actual R1,53s |
| Focused final-code regression class | PASS |12 tests/0 failures/errors/skips, BUILD SUCCESS1m18s; .gradle/saved-edit-rollback-20261026/after/core/test-results/test; focused.log outside repo |
| Ordinary core check | PASS |Offline :core:check BUILD SUCCESS28s;326 tests/52 suites,0 failures/errors/skips. Main/test compilation reused identical focused-run cache; ordinary test task executed. .gradle/saved-edit-rollback-20261026/final/core/test-results/test; final-check.log outside repo |
| Independent source review | PASS, no blocking finding |Separate read-only /root/saved_edit_review, owner-authorized, no edits/builds/tests/private evidence or delegation |
| Diff whitespace/source scope | PASS final |git diff --check; only the reserved production file/new synthetic class; final SHA256 exactly match reviewer pins |
| APK/signature/phone | NOT RUN |Explicitly outside owner scope; source publication is not installed/delivered acceptance |
| Source publication | OWNER APPROVED |Only Repositories.kt, CompletedMatchRecoveryTest.kt and this sanitized checkpoint; exact commit/remote receipt belongs in current coordinator/shared records |

Focused cases cover actual UI history-only save/reopen, failed-transfer retry using all edited fields, history newer/equal/older than outbox, equal-revision conflict preserving both stores/no export, stale caller export, conflicting obsolete revisions, explicit note clearing, adjacent-game isolation/retry idempotency, terminal exported-state/manual revisioned filename, stale history writes, stale terminal outbox stage, and an edit during recovery transfer surviving reopen. Existing MiscTest/MatchJsonExport tests remain unchanged. Ordinary private-fixture exclusions are unchanged; no skipped test added to conceal a failure.

Existing offline toolchain was reused without installation/config changes. Fresh before/after/final build roots follow the existing init-helper pattern and preserve prior locked outputs; no cleanup/move/delete of existing reports/APKs/fixtures/caches. No Android assemble/signing task invoked. Core has no separately configured lint plugin/task; :core:check is its configured verification lifecycle, with compiler checks and ordinary tests. Existing redundant-Json/deprecation warnings retained.

## Independent review and exact source pins

Reviewer independently read source diff, new synthetic tests, JsonFileStore, actual UI Save/startup/export dependencies and existing regressions. No blocker; newest history protected, highest equal-revision conflicts refused, stage-before-history ordering retained, explicit empty and terminal retries consistent. Reviewed/tested SHA256:

- Repositories.kt:06138841ae74edaf189cc40bac91f6a463a1133e5d0cb2cd262f64754dffd903.
- CompletedMatchRecoveryTest.kt:bc3b4112192c28f45dd9d755c99273c433c8e22584d12fc20018ceba2d8e9116.

Review limits retained: tests do not deterministically suspend exactly between first outbox stage and history upsert, although that interleaving is protected by the atomic second comparison; crash boundaries are source-reviewed rather than fault-injected. Existing locks protect shared repository instances, not independent concurrently writable instances. Equal-highest-revision conflict preserves both payloads and propagates an exception; no new user-facing conflict-recovery flow. An already-running transfer may export its earlier snapshot while a later edit remains safely saved for subsequent export; this unit does not promise automatic export of that edit.

## Publication and next owner step

Source-only; not delivered in a new APK. The original incident's cause/provenance remain unknown; reproducing the synthetic path does not prove the historical note loss used it. Detailed original-evidence investigation remains local and is excluded from this sanitized publication. Broader HSC issues remain OPEN/priority UNASSESSED; prior class-label phone acceptance and inventory correction retained. Validation match remains outside coaching.

BUILD_NOTES_STANDARD applies to the approved source-only handoff. Existing Resource Exchange/canonical HSC-009/010 entries must distinguish source publication from APK delivery/phone acceptance, retain history and broad OPEN/UNASSESSED requirements, and link the actual verified source commit. Final ordinary check/source pins/document readback are verified; no redundant tests are needed for unchanged source. Final publication/shared verification is a separate coordinator receipt, not assumed from approval. Next owner decision after publication is a combined APK preparation/checks/new Development package/Drive delivery/hash gate if the owner wants the fix on the phone. No APK or phone request follows automatically; fused-note projection remains separate.
