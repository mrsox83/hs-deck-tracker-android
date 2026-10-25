# Numeric reveal-recipient fix — review and validation

## Publication receipt — October24

Owner subsequently approved publication/reconciliation. Source5214f06cb2e7368c92f87a086d5aa28d1d1f81de pushed/remote hash VERIFIED; reviewed product unchanged. Resource Exchange same-ID sanitized status and canonical native backlog guarded append readback VERIFIED with old history preserved. Publication checkpoint records links/scope/sync qualification. Supersedes source-local/publication-pending wording below, not test limits or APK/phone gates. No /8 APK/old-artifact rewrite/merge/deployment.

Owner approved bounded parser implementation/regressions after read-only investigation, then explicitly approved one independent read-only reviewer. No publication/APK authorization. Base86e9ad17fc85b75bd91cbff450503c6addbc0292, feature/mobile-fusion-offline. HSC-006/010 remain OPEN, priority UNASSESSED.

## Source changes

One reveal-header recognizer now resolves positive numeric IDs or explicit-ID bracketed descriptors, including nested card-type text in names. Invalid/overflow/ambiguous recipients clear pending inline recipient. Valid reveal identity and following inline tags use the same recipient. SHOW visibility no longer falls back to permissive raw/preparsed IDs. Rejected/quarantined reveal headers cannot register aliases. Existing reset/restored-player quarantine paths retained. New generation producers offline-r2/8 and android-r3/8; schema unchanged, old saved artifacts untouched. No UI change or new permission.

Synthetic tests cover68→57→52→63 identity/controller/cost separation, numeric SHOW/CHANGE, empty identity, nested-name descriptors, invalid recipients, recovery, later named-tag alias bypass and reset nonbackfill. A blanket inline ENTITY_ID mismatch policy was considered but deferred: this slice prevents wrong attribution at the header; changing all existing inline-tag acceptance semantics would need separately scoped evidence/tests.

## Independent review

Reviewer reveal_recipient_review had source and synthetic tests only, no private evidence/build/edit. Initial actionable finding: rejected malformed header still registered a name alias, allowing later named tags to bypass rejection. Fixed alias ingestion and added regression test. Focused re-review: original path closed, no further blockers within numeric/descriptor recognition, visibility, malformed-header containment, reset/quarantine scope. Not runtime acceptance.

Reviewed SHA256 reducer c83c0a5a9e125970b03606f2054b510ed8bdbcfb03897bee1bdc10ae768275f9; coordinator6ddb09989373f79dd4d62a6be0f21bedd324d07b2880babe246b9192dcd0f265; RevealRecipientTest d796278861c4d5616ffa80b372e5732d5e1aba84c0a0f9b247787d6bfb58b09c.

## Final validation — CHECKED AND PASSED, SOURCE-LOCAL ONLY

Final exact-source run BUILD SUCCESSFUL2m8s. Fresh final-validation-2 XML independently counted:308 ordinary tests/51 suites,15 broader private tests,1 private two-phone-upload test,1 exact M.O.T.H.E.R. test; all zero failures/errors/skips. Ordinary/phone/corpus tasks executed; exact M.O.T.H.E.R. restored FROM-CACHE from independently executed54s PASS on identical source/test inputs; Android compile UP-TO-DATE from prior successful compile, production source unchanged. Reviewed reducer/coordinator/synthetic-test hashes rechecked and match receipt above. git diff --check PASS (CRLF notices only).

Both corrected phone reconstructions have zero duplicate-position hands;340/3303 inline tag recipient proofs verified,273/657 checkpoints, canonical event/snapshot counts unchanged, provenance validation clean and old /7 artifacts decoded/preserved. Entire48-match corpus passed independent raw inline-recipient attribution plus prior explicit descriptor-witness audit; revised diagnostic counts are thereby accepted. Original chronology/structure goldens all PASS unchanged. This removes the demonstrated parser contamination in source-only reconstruction, NOT universal hand completeness, automatic pairing, installed APK behavior or old-artifact repair.

Intake12%five-hour/52%weekly; final34%/55%, normal85% ceiling. No reset credit used. Local/remote baseline86e9ad1 verified before implementation; no commit/push. Four preexisting untracked helpers/private evidence retained. Canonical/shared reconciliation remains explicitly pending owner publication approval.

## Validation attempts — history superseded by final result above

Sandbox Gradle could not load native-platform.dll; host cached/offline toolchain used without configuration weakening. First build completed Android compilation but failed2 of307 ordinary tests: nested descriptor compatibility and permissive invalid SHOW visibility creation. Those paths corrected; failure XML preserved in ignored .gradle/reveal-recipient-20261024/first-attempt-reports. Source adjusted during that first run, so it is not an exact-final acceptance. An ignored private-test initialization attempt failed before tasks due rootDir scope; corrected to rootProject.rootDir. No tracked build configuration change.

Second host run compiled updated sources/tests and Android, but ordinary test execution was blocked before replacing results by Windows lock on old test-results/test/binary. Old XML was not counted as new acceptance. Switched only ignored test report/binary output paths to fresh final-validation-1 directories; no deletion, process stopping or tracked build changes.

First fresh-result run:308 ordinary tests PASS, Android compile PASS, private phone test PASS for both uploads:340 and3303 inline tag proofs,273 and657 hand checkpoints, zero duplicate hands, provenance validator clean. Originals unchanged; old /7 decode succeeds. Broad corpus14/15 PASS, failure only fixed diagnostic-count expectation: ZONE22610→19544, CONTROLLER566→75; position20665/unsupported288 unchanged. Chronology counts48matches/43completed/4truncated/21018events/29674snapshots reached unchanged assertions; later assertions after count failure were not yet accepted.

To justify the diagnostic count update, added an independent raw-header ownership pass to every corpus match: all non-restoration inline tag-history entries must belong to the exact raw block recipient, without calling production recipient code. Existing per-match raw-descriptor/prior-explicit proof audit remains. Only diagnostic count pin changed; chronology/structure goldens preserved. Initial corpus failure report retained in final-validation-1, final checks use final-validation-2. Exact M.O.T.H.E.R. test independently PASS in54s after adding the ownership audit; final complete results above supersede this historical pending stage.

One new private test checks both phone uploads using original hash-pinned ZIPs, immutable old /7 exports, exact recipient/inline raw-line attribution, corrected hand generation and provenance validation. Fixture pairing is explicit for the check, not automatic pairing acceptance. Private helpers/results stay ignored and are excluded from ordinary tests. This is source-local reconstruction, not a replacement phone artifact or automatic pairing acceptance.

## Gates

No commit/push/APK/upload/merge/deployment; raw and old exported artifacts preserved. Canonical HSC-006/010 root-cause and source-local fix reconciliation pending owner-approved sanitized shared update; no external write in this task. Broader hand completeness/observer knowledge/performance remain unaccepted. Next after passing checks: owner source-publication/shared-status gate, then combined APK preparation/checks/Drive-delivery approval separately.
