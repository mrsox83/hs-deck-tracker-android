# HSC-004 reset chronology checkpoint — 2026-10-18 Central

The owner approved a bounded event/snapshot chronology unit after a read-only HSC-004 review. Branch `feature/mobile-fusion-offline` began clean and synchronized at `cf873b3`. No APK, merge or deployment was authorized.

## Implemented boundary

- Added backward-compatible `chronologyEpoch` and `chronologyState` fields to canonical events and snapshots. Legacy artifacts decode as epoch 0 / CURRENT.
- Every observed `GAME_RESET` starts a monotonically increasing epoch. Earlier epochs remain immutable HISTORICAL evidence; only the final observed epoch is CURRENT.
- Reset clears stale raw turn, active controller, per-player indices and mutable entity state. The first post-reset snapshot is FULL and contains only state re-observed after the boundary.
- Gameplay rewind classification requires a fresh adjacent `USED_REWIND=1` observation for the reset actor. A sticky older tag is insufficient; other resets remain unclassified.
- Reset closes prior block ancestry so a CURRENT event cannot parent a HISTORICAL event.
- Stitched reconnect sources offset later epochs monotonically. A reset in the later source makes the earlier segment historical.
- Provenance validation checks nonnegative/nondecreasing epochs, final-versus-historical state, FULL epoch baselines and cross-state parent links.
- Passive compact summaries expose epoch/state and explicitly warn that historical events are not additional current activity.

## Independent review and remaining boundary

A cloud-agent read-only review found two pre-validation correctness risks: stale entity state could survive into a post-reset FULL snapshot, and persistent `USED_REWIND` state could misclassify a later reset. Both were corrected and regression-tested.

Epoch classification currently covers events and snapshots only. Choices and final aggregate entity history remain preserved but unclassified by epoch; consumers must not use those surfaces for current-state or hand reconstruction across reset. Broader played-first/played-second/reconnect corpus acceptance remains open, so HSC-004 is not universally DONE and HSC-006/010 is not unlocked by this checkpoint alone.

## Verification

- Focused reducer, continuation, legacy-codec, validator and summary tests: PASS.
- Full ordinary core suite: 225 tests across 37 suites; zero failures, errors or skips.
- `git diff --check`: clean except existing line-ending conversion notices.
- Normal output and the first isolated test-results directory encountered the known Windows file lock. A second fresh output completed focused validation; a third completed the full suite. Locked directories were not deleted or modified.

No APK, phone claim, existing-artifact rewrite, commit, push, merge or deployment was performed.
