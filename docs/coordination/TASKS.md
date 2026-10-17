# HS-FUSION task register

Coordinator-owned. Updated 2026-10-17. Source of truth for development ownership; no agent self-claims. Single cloud pilot maximum until reviewed.

| ID | Owner | Status | Scope / reserved writes | Dependency / acceptance |
|---|---|---|---|---|
| LOCAL-001 | Remote/local coordinator | DONE | Android UI, capture/reader, fusion reducer/models/import, export/persistence, all build/signing config, integration branch | Manual-export V4 phone/artifact gate accepted; exact source hash verified and one app history entry confirmed |
| CLOUD-001 | cloud-001-worker-1, single user-launched cloud chat | DONE | Only core/src/main/kotlin/com/stroexd/hsdecktracker/core/fusion/summary/**; core/src/test/kotlin/com/stroexd/hsdecktracker/core/fusion/summary/**; docs/coordination/reports/CLOUD-001.md | Accepted and integrated as `a81538f`; 14 focused and 211 ordinary tests pass locally; reservation released |
| COORD-001 | Remote/local coordinator | DONE | AGENTS.md; docs/coordination/** except assigned worker report; TODO.md/WORK_LOG.md/checkpoints/**; approved shared-note publication | Combined v6 phone gate accepted and documented in `11a1177`; no merge/deployment |
| LOCAL-002 | Remote/local coordinator | WORKING | Tracker capture-coverage provenance, fusion completeness containment, synthetic tests, coordination/checkpoint records | Completeness containment and bounded interval/gap instrumentation implemented;219 core tests and Android source compilation pass. Raw tracker/fusion/Power.log review remains mandatory after connector/H: recovery before resolution or APK; no merge/deployment |

## Assignment protocol

1. Coordinator checks live local/remote tip, dirty work, dependencies and file overlap.
2. Publish contract and this register before launch. Reserve paths even while READY; coordinator avoids writing them.
3. Issue one launch capsule: ID, exact40hex START_SHA, cloud environment, worker/chat identity, task branch, allowed paths, verification commands and expiry/review time. The task starts from that SHA, not default main or latest-by-assumption.
4. Record DISPATCHED and capsule details here before worker implementation. Worker acknowledges base/scope before coding. Missing identity/environment or unresolved placeholders means do not start.
5. Coordinator may continue only disjoint work. Shared model/interface/build changes are frozen while CLOUD-001 runs; negotiate a revised base/contract first.
6. Worker returns its own report and diff/patch/commit; only coordinator changes status to REVIEW. No automatic integration or release.
7. Coordinator reviews allowed-path diff, secrets/provenance, exact tests, compatibility and private gates where needed. Integrate only accepted work, then push/verify designated development branch. No merge/deployment.
8. DONE means acceptance verified, not merely code returned. Stop/cancel must be acknowledged before reassigning; expiry alone never creates a second worker.

States: READY_NOT_DISPATCHED → DISPATCHED → ACKNOWLEDGED → WORKING → REVIEW → ACCEPTED/CHANGES_REQUESTED → DONE. BLOCKED/CANCELLED preserve reason/evidence. No worker may edit this register to grant itself authority.

Code baseline for pilot planning:85fa84b566911bbfa57a2463f4e22b747dc413f2. Exact launch SHA must include these coordination documents and is supplied separately after publication. This is not a floating-start authorization.

## CLOUD-001 launch record — 2026-10-15 17:36 UTC

### Resume allowance — 2026-10-16 03:18 UTC / October15 Central

Owner relayed successful startup at d71c04e9b6b58125fd9dfe4f353ef24cf7cbdeca on cloud/cloud-001-summary-pilot, toolchain restored, no implementation/tests/push. Worker remains cloud-001-worker-1; no replacement chat authorized. Owner raised current five-hour ceiling to90%; live coordinator read14% used (weekly21%, not the governing ceiling). No reset consumed or authorized here.

Authorize one bounded implementation checkpoint: maximum20minutes elapsed from receipt, one focused test invocation and at most one ordinary core test invocation; stop earlier on completion/blocker/deadline. No repeated repair/test loops or automatic next unit. If incomplete, return partial patch and explicit failing/unrun gates. This is a work bound, NOT a promise of percentage consumption. If live usage becomes available stop at90%; user/coordinator must externally interrupt if shared usage approaches ceiling. Return for fresh coordinator usage check before any continuation. Local coordinator performs no concurrent implementation during this allowance.

Keep original d71c04e launch base; fetch this coordinator amendment for read-only verification via git show, not merge/rebase into the worker branch. Setup baseline must not silently replace it. Startup report file, if present, is the worker's own authorized change; preserve it. All other task restrictions and2026-10-16T17:36:00Z deadline remain. Assignment amendment publication is not evidence the user has delivered it or work has resumed.

### Acceptance — 2026-10-15 Central

The owner confirmed backlog readiness and relayed the bounded allowance to the existing worker. Worker commit `f4512ac6a0659da70ca2624072c5ee35219d1a42` changed exactly the three allowed paths and was cherry-picked as integration commit `a81538f`. Coordinator source/scope review found no blocking issue. Independent local validation used the verified cached Gradle 8.14.3 executable offline: 14 focused tests and the complete ordinary core suite of 211 tests across36 suites passed with zero failures, errors or skips. CLOUD-001 is accepted and DONE; its exclusive reservation and model/interface/build freeze are released. This accepts only the passive summary pilot, not full HSC-006/010 coaching completeness, app integration, phone behavior, merge to main or deployment.

Owner reports environment hs-deck-tracker-android published. Supplied setup report verifies baseline339515afcc780e4893ec4e8a96a3506c331df6a7, JDK21/Gradle8.14.3/SDK36/build-tools35.0.0,197 ordinary tests passing and clean checkout. Publication/fresh-task restoration is owner-reported, not directly observed locally; worker must verify restoration before implementation.

- Worker identity: cloud-001-worker-1; bound exclusively to the first cloud chat launched with the coordinator capsule. Record its actual chat URL in its own report. Never reuse this capsule for a second chat.
- Environment: hs-deck-tracker-android. Task branch: cloud/cloud-001-summary-pilot.
- START_SHA: exact hash supplied in coordinator's launch message after this dispatch-record commit is published. It must be the commit introducing this record, not the earlier setup baseline. This external pin avoids a self-referential commit hash.
- Review/stop deadline: 2026-10-16T17:36:00Z. Stop earlier on completed pilot, blocker or usage ceiling; expiry never permits reassignment without acknowledged stop.
- Conservative shared five-hour ceiling75%; live local observation62% at dispatch preparation. No reset authorized by this assignment. Cloud must read fresh usage if available; if unavailable, return startup verification only and wait for a coordinator-authorized bounded work allowance rather than assume an independent quota.
- Fetch pinned commit before running any baseline-pinned install script. Existing install.sh may intentionally reject a changed SHA: do not weaken it, modify tracked build files or silently revert. Reuse prepared toolchain via activate.sh, and report restoration/setup blockers.
- Startup response must state exact HEAD, branch, environment, worker identity, actual chat URL and allowlist before edits. Then proceed within contract if all gates, including usage, pass; register updates remain coordinator-owned.
- Dispatch means assignment issued for human relay, NOT proof the cloud chat is running. No direct cloud launch occurred locally. Coordinator avoids reserved directories and freezes model/interface/build changes until return.
