# HS-FUSION task register

Coordinator-owned. Updated2026-10-15. Source of truth for development ownership; no agent self-claims. Single cloud pilot maximum until reviewed.

| ID | Owner | Status | Scope / reserved writes | Dependency / acceptance |
|---|---|---|---|---|
| LOCAL-001 | Remote/local coordinator | WAITING_PHONE | Android UI, capture/reader, fusion reducer/models/import, export/persistence, all build/signing config, integration branch | v4 phone checks in checkpoints/mobile-fusion-manual-export-v4.md; no new changes merely because waiting |
| CLOUD-001 | Reserved cloud worker; identity NOT yet assigned | READY_NOT_DISPATCHED | Only core/src/main/kotlin/com/stroexd/hsdecktracker/core/fusion/summary/**; core/src/test/kotlin/com/stroexd/hsdecktracker/core/fusion/summary/**; docs/coordination/reports/CLOUD-001.md | tasks/CLOUD-001.md; verified repository cloud environment + exact launch SHA/worker/branch required |
| COORD-001 | Remote/local coordinator | ACTIVE | AGENTS.md; docs/coordination/** except assigned worker report; TODO.md/WORK_LOG.md/checkpoints/**; approved shared-note publication | Publish startup/task/handoff protocol; no cloud dispatch without verified target |

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

## Current dispatch blocker

The owner confirms cloud access is enabled but no environment has been set up. The available project listing identifies a local Matches directory (not the actual Git checkout) and a ChatGPT Hearthstone project; neither is a verified repository coding environment. No real cloud environment discovery/launch API is available in this chat. Do not confuse the simulated environment setup tool or a generic cloud chat with real provisioning. Create/select a repository-connected environment, verify its toolchain and base, then issue the launch capsule. No cloud worker/task has been launched.
