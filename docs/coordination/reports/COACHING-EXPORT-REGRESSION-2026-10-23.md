# Coaching export — coordinator regression review, October23

This is coordinator test triage, NOT independent source review. Source remains uncommitted on feature/mobile-fusion-offline, HEAD1203094e5f0ca44e157db812d1ec33f944397d0b; current remote independently matches by ls-remote. No source/test baseline changed during this review. Dirty source/docs/helpers preserved. No APK, push, merge/deployment or canonical issue closure.

## Executed broader gate

Mounted <PRIVATE_EVIDENCE_ROOT> is readable outside the sandbox; earlier sandbox denial did not mean unavailable on host. Existing :core:realFusionFixtureTest ran offline with cached Gradle8.14.3, isolated .gradle/coaching-review-corpus-20261023 outputs and1GiB test heap.12 private tests:9 pass,3 fail, no errors/skips; BUILD FAILED1m25s. Prior282 ordinary and one strict private M.O.T.H.E.R. passes/Android compilation still apply to unchanged source; not rerun here.

## Findings requiring reconciliation, not automatic expected-value edits

1. RealFusionFixtureTest:74 expects all three slot proofs for seven cards to be supplemental descriptor evidence. Actual filtered set is empty for a restored card. Exact eight-slot order assertion at66 passed; fresh FULL_ENTITY Updating restores explicit tags, changing the supporting evidence. Need independently verify actual proof lines/rules, then decide whether this historical fallback-only assertion should be superseded. Do not remove order or raw proof checks.
2. RealFusionFixtureTest:135 expected Priest pairing PENDING as a characterization, actual ACCEPTED. Candidate score5: endpoint11s, three ordered card anchors; other candidate score2 rejected for endpoint>120s/result conflict. The parser fix can change observed identities/anchors. Need compare exact restored evidence and candidate criteria before approving a newly accepted pairing. Do not simply flip the expected status.
3. RealFusionFixtureTest:385 descriptor diagnostic audit changed: expected ZONE22643/ZONE_POSITION20650/CONTROLLER566/UNSUPPORTED288; actual22610/20665/566/288. Restored explicit state can legitimately change supplemental disagreements, but raw-witness comparison is required before updating a golden. Prior assertions for48 records,43 completed,4 truncated,21018 events and29674 snapshots passed. Subsequent assertions after385 were not reached and are NOT claimed passing.

Reports retained .gradle/coaching-review-corpus-20261023/core/test-results/realFusionFixtureTest. No real raw logs copied to source or shared review prompts.

## Independent reviewer gate

One read-only review sub-agent permission requested through user question, answer pending. No reviewer launched and no independent no-blocker result exists. Review scope when authorized: source diff/new CoachingHandHistory and tests, canonical FULL_ENTITY Updating handling, action linkage, provenance validator, serialization/backward compatibility, runtime/memory implications, and the above three observed regression changes. No private logs or H: access for reviewer; coordinator owns source-backed fixture checks. Reviewer must not edit/commit/build/push or spawn others. Return concrete findings/line references and explicit limitations.

Next: obtain reviewer permission, inspect source independently, then coordinator reconciles exact evidence and regression gates before source publication/APK. Preserve existing golden expectations until justified.
