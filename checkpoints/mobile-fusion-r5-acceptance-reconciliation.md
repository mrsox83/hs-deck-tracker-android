# R5 acceptance reconciliation — 2026-10-12

HS-FUSION only. Existing HSC-009 journal/HSC-013 coaching context; UNASSIGNED modal report owned/resolved as described below, no invented ID. Development branch feature/mobile-fusion-offline, start local/remote597fb7980e1d1e2afc26b2285dad9f5a3f485435 clean. Current manual-v5 packagecom.stroexd.hsdecktracker.fusiontestr4; prior174 tests/APK/signature evidence unchanged, no new build/test run. Usage12% five-hour/10% weekly,90% ceiling, no reset.

## Device observations

- Modal blocker: owner confirms controls reachable/full details readable after v5. Existing5ac9586b re-export parses and is byte-identical to original. Reported blocker accepted/resolved; large-font/small-window variants not independently claimed. Canonical backlog not edited by this session; shared note carries acceptance.
- Owner explicitly reports #3 interrupted-retained retry PASS. Accepted as user-reported behavior; exact raw-saved/pre-fusion timing and no-new-read boundary not independently captured. Do not demand a race against fast fusion.
- Owner initially reported disabled buttons with Shizuku stopped, then corrected: fused JSON export works without Shizuku. Latest9421785e JSON arrived17:09 Central, ACCEPTED,539 events, SHA2563d84cd84ac1497d791d685f97325f7a94af598bf9bbdeb9bb9962156bd0398b4. This passes saved-export independence and actual arrival, not permission denial or Binder-death during a read.
- Switching to Shizuku backgrounds/closes the foreground-only manual dialog by design. This is not itself a crash or live Binder-death test. Restart/reopen/reconnect is a separate finite check.

## Uploaded inventory (one row per logical game)

| Tracker ID | Logical ID | Match | Events | Coverage |
| --- | --- | --- | --- | --- |
| 861b778a-104c-4d6d-b741-07acea8ba6a7 | e14389a4d8e0258210404db2203d1bd7 | DK/Rogue LOSS |354| First campaign game; owner history/status acceptance recorded |
| 40658727-060c-4b27-9406-6782ae16dceb | 1b0fec36d20acb0eeac8e432cd9072b0 | DK/Warrior WIN |188| Accepted; extra selected source is not automatically timeline coverage |
| 86eb2b3d-dee2-4d02-9d17-9aa4e8a0aa75 | 908810a8345283392a204cf9e4a160be | DK/Warlock WIN |207| Accepted; other candidate retained |
| f2af39db-b51d-4bdd-a43a-5fe2477fa12d | 0102e83964067daec883a187d0fa8eb9 | DK/Priest LOSS |172| Manual reconnect: later fragment only, no accepted stitch; old artifact predates warning |
| 5ac9586b-19b0-4a06-98f6-91dd23c4e87a | d4d4dd3dbb19ee9c02d6e592c93c8678 | DK/Warrior WIN |402| One unincorporated source warning; original and (1) are identical |
| 9421785e-e4d4-406f-881a-eb788bbd406a | 31ad1c64ad48915ca95b22135a249211 | DK/Priest WIN |539| Two unincorporated sources warning; Shizuku-off export owner-confirmed |

All six parse as hs-fused-match/1, revision1, ACCEPTED. Artifact-level presence/count does not prove five consecutive failure-free games, per-game no duplicates in phone history, fully reconstructed reconnects or network recovery. No further games requested for numerical count. No processed coaching ledger/analysis performed. Owner-authorized campaign coaching eligibility supersedes its former test labeling; older diagnostics exclusions remain. Extra uploads beyond the campaign are not automatically reclassified from filename alone. Coaching agent must reconcile actual eligibility and aliases once, retain warnings, and not count re-exports as games.

## Remaining finite steps — no new APK required

1. Permission denial/regrant, after gameplay: keep Shizuku running, disable only the Fusion Test app's authorization in Shizuku's authorized-app management. Reopen the pilot. It should request authorization; deny once. New reads must not start or claim success; existing fused JSON remains exportable. Then authorize and Connect/refresh; sessions should appear without an error. Do not change the original tracker's authorization/data.
2. Restart/reconnect, idle not mid-fusion: connect/refresh in the pilot, then switch to Shizuku and stop it. Background closure expected. Reopen pilot: new reads unavailable, saved JSON works (already passed). Restart Shizuku normally, reopen pilot, confirm finished-match checkbox and Connect/refresh. Success is sessions returning, no crash or duplicate history. This tests restart recovery, not live Binder death while copying; record that boundary rather than claiming more.
3. Existing export route network recovery: keep committed JSON locally, attempt Drive export offline only if convenient; accept an honest provider error or local queued write, not remote success. Restore network and export/reconcile once; hash/arrival and no duplicate logical-game counting checked independently. No app-side native Drive uploader exists; do not infer remote status from SAF write.

Owner can also confirm whether uploaded campaign games retained status and single history records. No forcing phone failures during normal games, no redundant repeated tests. Automatic candidate discovery remains an approved queue item, but no implementation in this documentation checkpoint; automatic collection OFF/R6 excluded. Notify owner before transition beyond R5. No merge/deployment.
