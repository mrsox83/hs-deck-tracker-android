# Mobile fusion R0–R2

## Authoritative current state — 2026-10-15

This section is the restart source of truth. `docs/coordination/TASKS.md` owns active assignments and reserved paths; `docs/coordination/HANDOFF.md` owns the current integration/phone handoff. The dated sections below are retained as an append-only project history. An unchecked historical item is not active unless it is repeated here or in TASKS/HANDOFF.

- [x] Repository `mrsox83/hs-deck-tracker-android`, branch `feature/mobile-fusion-offline`; code baseline `85fa84b566911bbfa57a2463f4e22b747dc413f2` with coordination-only commits following it. Verify the live local/remote coordination tip before new work. Codex's sandboxed bundled Git can fail work-tree comparison and must not be treated as repository corruption without an external read-only check.
- [ ] `LOCAL-001` current phone gate: validate the manual-export V4 delivery named by HANDOFF—export the already-fused game JSON and the exact accepted-source troubleshooting ZIP without requiring a live Shizuku reader; verify the enclosed source/session hash rather than the filename. Do not repeat the closed overlay or five-game campaigns.
- [x] `CLOUD-001` passive summary pilot accepted and integrated as `a81538f`: exact allowlist, coordinator source review,14 focused and211 ordinary local tests pass; reservation/freeze released. This foundation does not by itself complete HSC-006/010 or add app integration.
- [ ] HSC-004 chronology remains broader than the fixed cross-match owner-leak case. HSC-006/010 compact coaching summary acceptance depends on verified chronology and the cloud pilot; neither is complete merely because source or an APK exists.
- [x] Conservative user-triggered discovery and its bounded search/review/confirm implementation exist. Historical text that says discovery is only queued or unimplemented is superseded.
- [x] Manual R5 permission/regrant, reconnect, export, persistence and no-duplicate checks were later owner-accepted within their recorded limits. Historical pending manual campaigns are superseded; exact live Binder-death timing and five consecutively instrumented error-free games were not proven.
- [ ] R6/background automatic collection, native Drive upload, merge and deployment remain excluded.
- [ ] Before each phone-validation or release-candidate APK, run the shipped-artifact REA gate in `checkpoints/mobile-fusion-rea-artifact-gate.md`. This supplements source/build/signature and phone gates; static analysis never substitutes for runtime acceptance.

## Remote/cloud coordination — 2026-10-15

- [x] Owner-approved coordinator protocol, task ownership register, handoff and bounded CLOUD-001 contract prepared in `docs/coordination/`; root `AGENTS.md` supplies startup safeguards.
- [x] Environment hs-deck-tracker-android setup report reviewed: exact339515a baseline,197ordinary tests pass, clean checkout; owner confirms publication. Fresh-task restoration still a startup gate.
- [x] CLOUD-001 assignment executed by the single recorded worker from exact launch SHA; no duplicate/replacement worker.
- [x] Returned task commit `f4512ac6` reviewed and integrated as `a81538f`;14 focused and211 ordinary local tests pass. No merge to main or deployment.
- [ ] Existing manual-export-v4 phone gate remains pending; cloud preparation does not imply phone acceptance or complete HSC-006/010.

## HSC-004 chronology — 2026-10-14

- [x] Source fix for session-global player-name/controller leakage: match-line scoping, time-gated alias application, synthetic controller-swap/no-future tests. See checkpoints/mobile-fusion-chronology-investigation.md; not a shipped or exact-Warrior-verified fix.
- [x] Exact18:40:30 raw ZIP arrived/hash verified. Game3 has unmapped named player after UNKNOWN HUMAN PLAYER; old reducer carried opponent owner forward. V4 clears unresolved owner/indices and preserves diagnostics, raw turns and existing artifacts. Exact15signal/290event replay passes; fusion versions /2. HSC-004 broader mapping/classification remains open, no parity inference. See checkpoints/mobile-fusion-manual-export-v4.md.
- [ ] V4 phone: fused-game export-first layout, JSON and exact accepted-source troubleshooting ZIP without reader; Advanced available, cancellation honest. Verify uploaded ZIP's session/hash, not filename. Old artifacts are NOT regenerated. No repeat overlay campaign.
- [x] Daily shared-feedback check2026-10-15 completed; quiet9AM read-only triage automation active. See checkpoints/shared-feedback-daily.md. No new implementation authority from shared notes.
- [ ] HSC-006/010 compact coaching companion still depends on verified chronology and later consumer/app acceptance. The accepted CLOUD-001 passive renderer is only a bounded foundation, not coaching-complete coverage.

## Security audit — 2026-10-14

- [x] Investigate owner-authorized GitGuardian email: linked release hashes independently verified; false positive at the reported location. Scan source/history/local deliveries; no actual credential found within documented bounds. Add preventive signing-file ignores and token-free CI push authentication. See checkpoints/mobile-fusion-credential-audit-2026-10-14.md.
- [ ] Release CI credential-helper runtime validation remains unperformed; do not trigger a release solely for this audit. GitGuardian dashboard disposition/provider access-log review not performed or required to verify these public hashes.

## R5 authorized — post-match collection, 2026-10-10

- [x] Owner-approved three-session discovery batches: first3 ranked sessions, explicit next3, cached bounded candidate/reducer results and saved bundles reused without repeat Shizuku reads; cumulative6/64MiB/24matches/32MiB caps and per-batch60s cooperative deadline. Stops lead status; timeout candidates preserved but remain unconfirmed. Source checkpoint mobile-fusion-r5-batches.md; not installed yet.
- [x] Next-build overlay QoL source: minimized bubble moves freely within display bounds, no edge snap; clamps on rotation. Position retained for current overlay lifetime, not a new persisted setting. Phone acceptance pending.
- [x] Combined-v2 overlay phone acceptance: owner reports overlay issues resolved. No further overlay campaign; exact mechanism/device generality not established. See checkpoints/mobile-fusion-r5-v2-phone-feedback.md.
- [x] V3 recovery source/tests/APK: Review this game's saved logs immediately reviews all games in chosen saved sessions, no new Shizuku read/collection; warnings/strict pairing retained, Back to all search candidates available.195ordinary/3private tests pass including latest414event/562snapshot upload. See checkpoints/mobile-fusion-r5-saved-review-v3.md.
- [x] **Superseded V3 phone checklist:** later saved-review/discovery builds and the accepted manual campaign replaced this exact gate. Its null-descriptor cause remains an unresolved historical unknown; supersession is not a claim that every V3-specific scenario passed.

- [x] R5 manual acceptance owner-confirmed: denial blocks reads, regrant restores connection/fusion; owner reports restart/reconnect, network recovery, persisted statuses/no duplicates all PASS. Artifact arrival/identity inventory verified separately. See discovery checkpoint; no further manual phone loop requested.
- [x] Owner approved reviewed user-triggered match-discovery scope on2026-10-12. Core policy/preview and Android search/confirm UI now implemented; discovery phone acceptance pending. Six sessions/64MiB raw/24 matches/60s cooperative budget; no closest-time pairing or score-only winner among plausible games.
- [x] Discovery integration source/tests/APK: retained per-session search, shared evaluator/reconnect lineage, candidate summaries/manual scope review and explicit confirmed fusion. Limits/cancellation/incomplete states and artifact-preservation guard tested. See mobile-fusion-r5-discovery-integration.md. Local toolchain repaired from verified official archives; no source SDK changes.
- [x] **Superseded discovery-v1 gate:** later discovery/overlay and saved-review deliveries replaced this exact APK checklist. The bounded discovery behavior exists, while any unperformed v1-only scenario remains historical rather than silently passed. No R6/background collection.

- [x] Triage 2026-10-12 Shared Exchange UNASSIGNED modal/export blocker to HS-FUSION: unbounded status consumed the dialog body. Compact status, expandable bounded full details and one scrolling body implemented; artifacts and pairing untouched.
- [x] Manual-v5 reported modal blocker accepted: owner confirms buttons reachable/full details readable; re-export arrived byte-identical. No blanket enlarged-font/small-window coverage claimed. See r5 acceptance reconciliation checkpoint.
- [x] Owner reports interrupted-retained retry (#3) passed; saved fused JSON export with Shizuku stopped also passes, Drive arrival parsed. Exact interruption boundary not independently instrumented.
- [x] Remaining finite R5 manual checks owner-reported all PASS after permission denial/regrant. Six unique accepted exports verified; five consecutive error-free sequence is not independently instrumented. No more games requested solely for file count.

- [x] Screenshot9362 pending diagnosis: PowerClock's wrapped twelve-hour threshold wrongly shifts early-morning logs to tomorrow. Correct non-wrapping rollover and reproduce unique first-game pairing in exact two-game bundle;172 ordinary, one exact private, six full fixture tests pass.
- [x] Prepare owner-requested full-width export/retry buttons with usage guidance, success-conditioned JSON and unnecessary-retry disabling; saved status/coverage restored on reopen. No automatic session search added.
- [x] v4-only phone request superseded by v5/manual closeout. Exact older4ae366a8 recovery export not independently found; pinned midnight regression passes. No redundant replay/phone gate requested. Historical instructions retained in v4 README.

- [x] Diagnose manual-record reconnect f2af39db: two stitch guards fail (choice anchor, raw-turn continuity); accepted timeline uses later fragment only. Preserve guards and implement persisted/artifact partial-coverage warning;169 ordinary tests and two private gates pass. See mobile-fusion-r5-manual-reconnect.md.
- [x] Partial-coverage warning shipped in v4/v5 and observed in uploads; full status details phone-confirmed. Existing artifacts are not retroactively rewritten. Missed automatic record remains an observed issue with unproven cause, no speculative capture change.

- [x] Manual-v3 game 1 phone UI acceptance: persisted status, already-fused retry without duplicate history, visible/readable status panel, and Stop tracking with confirmation checkbox unchanged. Owner confirms all four. Prior artifact inspection verified ZIP/JSON Drive arrival and accepted lineage for logical match e14389a4d8e0258210404db2203d1bd7.
- [x] Campaign/recovery manual closeout accepted on owner reports and uploaded identity inventory; exact interruption timing/live Binder death during a copy not independently measured. Owner made this five-game campaign coaching-eligible; older exclusions intact. Latest discovery checkpoint supersedes stale pending checklist text.

2026-10-11 owner resumed bounded preparation while five normal-use game reports are pending. Keep the installed manual-v1 APK unchanged; no automatic collection or R6. See `checkpoints/mobile-fusion-r5-preparation.md` and `checkpoints/mobile-fusion-r5-five-game-checklist.md`. This supersedes the pause for this preparation only.

- [x] Owner-requested Tracker toolbar OpenInNew shortcut now opens the same BackgroundSetupDialog as Settings instead of directly launching Hearthstone. Accessibility/settings and screen-sharing actions reused, no new capture-policy selector. Source prepared for next needed APK; phone check pending.
- [x] Source/build unit: Stop tracking above the finished-match checkbox, reusing existing tracker/overlay stop. Confirmation remains separate; no collection side effect. Phone validation pending in manual-v2.
- [x] Owner-authorized manual-v2 export source: explicit Android save picker for retained companion-contract ZIP and match-owned fused JSON, cancellation/failure/provider-write feedback, private originals retained. 167 ordinary tests/one reconnect gate pass; APK result/delivery recorded in export checkpoint. Phone/provider/Drive arrival remains pending; automatic candidate discovery is not included.
- [x] Screenshot9344 follow-up: v2 fused export rejected valid logical-ID filename. Correct derived-ID guard and regression using actual coordinator/writer; move contrasting status above scrolling controls. v3 build/delivery facts in mobile-fusion-r5-export-v3.md; device export still pending, not successful based on source/tests.
- [x] **Superseded queue entry:** owner approved conservative automatic candidate discovery (2026-10-11). The later bounded discovery policy, search/review/confirm implementation and integration APK supersede the statement that this was only queued or unimplemented. Remaining phone acceptance is tracked in the authoritative section and the later discovery checkpoints; no closest-time guessing, continuous capture, automatic post-match retrieval or R6 was authorized by this entry.

This section supersedes historical R5-not-authorized statements below. Owner approved the reviewed R5 plan with the existing 90% five-hour ceiling. R6, continuous tailing, merge and deployment remain excluded.

- [x] Inventory the pinned exporter v0.4 source and restricted read-only reader; record provenance and independent lifecycle requirements in `checkpoints/mobile-fusion-r5-post-match.md`.
- [x] Implement and test bounded snapshot primitive; publish foundation checkpoint (remote publication recorded in session handoff).
- [x] Implement restricted Shizuku client/reader with explicit permission request, Binder/death states, bounded connection wait and close/unbind; compiled and connected to foreground manual UI. Phone behavior still pending.
- [x] Wire foreground manual dialog lifecycle to reader; no automatic retry or capture/Accessibility dependency. Android compilation passed. Permission denial, Binder loss, close/reopen and timeout remain phone gates.
- [x] Reuse pinned parser/bundle contracts, private raw retention and collection/recovery journal feeding existing conservative importer. 160 ordinary tests plus required private reconnect round-trip passed; no newest-match guessing or tracker-record creation.
- [x] Add optional manual Collect logs and fuse, explicit retained/local/failed/pending states, retained-log retry and companion fallback. Existing destinations/data preserved; phone acceptance remains pending.
- [x] Build/check isolated manual-v1 APK, stage README/checksum in Development/mobile-fusion-r5-manual-device-validation-v1 and alert owner. Staged APK hash matches local build; no claim of phone installation or independently verified cloud sync.
- [x] Initial phone observation: screenshots show two retained raw logs, zero read failures and Fused locally; owner confirms status survives detail reopen and no duplicate history records. This is bounded manual-path evidence, not full R5 acceptance.
- [x] Owner confirms background closes dialog, explicit reconnection is required and succeeds without errors; retained retry without reconnect displays Fused locally/no cloud upload. Existing-fused preservation path verified, not interrupted raw regeneration.
- [x] **Superseded by later acceptance:** the bounded permission/regrant, restart/reconnect and interrupted-retained retry checks were subsequently owner-reported PASS within the limitations recorded above. Exact live Binder death during a copy remains uninstrumented and is not silently promoted to proven.
- [x] Source preparation: readable session date/time plus selected game's timestamp, and explicit already-fused/retry confirmation. 165 ordinary tests, pinned reconnect gate and Android compilation pass. Not delivered/phone-verified; bundle into next needed APK, not a standalone polish build.
- [x] **Superseded by campaign closeout:** six unique accepted exports, retry behavior and separately verified Drive/artifact arrivals closed the requested campaign. This does not claim five consecutively instrumented error-free games or erase per-artifact coverage warnings.
- [ ] R6 gate: automatic post-match collection using the journal/outbox remains excluded and requires a new explicit phase decision. Manual acceptance and user-triggered discovery do not authorize it.

Paused at owner request, 2026-10-10. Last sampled five-hour usage77%, ceiling90%, reset unused. No automatic resume scheduled. See `checkpoints/mobile-fusion-r5-pause-handoff.md`.

## Owner-authorized catalog bug follow-up — 2026-10-10

- [x] Full catalog lookup, separate constructed/collectible recognition and safe legacy-cache fallback implemented. Exact warm dead deck-code and live downloaded-catalog gates pass; 144 ordinary tests. R4 closeout remains in force; no R5 work.
- [x] Owner phone gate for v7 passed: owner confirms both card names resolved, deck remains 30 cards and history is intact. No re-import/reinstall or new match required. This verifies the bounded catalog fix, not general recognition accuracy or measured performance. See `checkpoints/mobile-fusion-card-catalog-fix.md`.

Approved scope: offline mobile evidence fusion only. No merge, deployment, PC adapter, or R3–R6 Android integration.

- [x] Verify shell execution and requested remote branch head.
- [x] Check out `feature/mobile-fusion-offline` at `d3bc0fb15c6cf4e29ba75fc0aac54b1ee75541bd`, preserving all four tracker patches.
- [x] Preflight: establish GitHub write authentication. Real pushes through `239ee23` succeeded and the remote branch tip was independently verified.
- [x] Preflight: pass `./gradlew :core:test` with downloaded dependencies and writable Gradle/Android caches.
- [x] R0: obtain exporter v0.4 source, catalog/schema inventory, and private mobile fixtures from plan references; record exact hashes and independently verified expectations.
- [x] R0: record installed tracker build/signing/source uncertainty without claiming the branch proves installed APK identity.
- [x] R1: implement versioned evidence schema, adapters, identities, conservative pairing, replay deduplication, and compact output with provenance.
- [x] R1: verify shuffled/repeated import, composite tracker rejection, ambiguous pairs, and partial-result coverage.
- [x] R2: implement time-scoped entities, block ordering, counters, separate choice stages, turn boundaries, and snapshots with unknown fields retained.
- [x] R2: assert critical mobile facts and synthetic edge cases; verify every accepted claim traces to exact evidence.
- [x] Run relevant core/schema/build checks and record reviewable checkpoints; preserve legacy exports and tracker behavior.

Current implementation checkpoint: the approved offline R0-R2 core is implemented and locally verified. Every accepted pairing characteristic and non-unknown typed claim is required to resolve to a known fused source and an exact source line or tracker JSON pointer; entity histories, canonical events, choice stages and snapshots are checked too.

Reconnect/session-rollover checkpoint: conservative two-session continuation is implemented for the HSC-004/HSC-009 evidence while remaining inside offline R0-R2. A stitch requires bounded clocks, the tracker endpoint, an incomplete non-truncated earlier source, an evidenced completed later source, a uniquely resumed open choice, both player/controller identities, stable entity identities and raw-turn continuity. The fused artifact retains both original sources, exact line/JSON-pointer evidence, parent lineage and an explicit gap diagnostic. Weak candidates remain unstitched.

Real-fixture checkpoint: the exact Rafaam tracker/exporter pair passes deep assertions, including wrapper/inner hashes, event and choice counts, separate Raze/Enthrall stages, action ordering, target-zero semantics, clock normalization and conservative pairing. The private corpus gate also pins and parses all 32 current tracker records and reduces all 48 source matches across 21 current exporter bundles, covering 21,018 canonical events and 29,674 compact snapshots. Unsupported higher-level game semantics remain deliberately unknown.

Remote write authentication is proven for `feature/mobile-fusion-offline`. The isolated lifecycle and sparse-snapshot checkpoint supersedes the prior pause handoff. No merge or deployment was attempted.

## Assigned for the next Codex window

Owner: next Codex session on `feature/mobile-fusion-offline`, after refreshing usage and confirming the branch tip.

- [x] R0-R2 hardening: measure full real-artifact stream decoding in an isolated consumer lifecycle that does not retain the reducer/source graph; record output size, memory conditions and pass/fail under an explicit 512 MiB heap.
- [x] Characterize dominant artifact sections and implement schema-compatible sparse snapshot deltas with full checkpoints, preserving provenance and defaulting legacy artifacts to full-state semantics.
- [x] Re-run `:core:test`, `:core:realFusionFixtureTest`, and the isolated lifecycle gate; record the results and push the completed checkpoint.

The exact Rafaam artifact now encodes to 11,033,664 bytes instead of 139,607,285 bytes. A fresh isolated consumer decoded all 1,144 snapshots in 532 ms with 73,551,360 bytes used after decode under a 536,870,912-byte maximum heap; lazy reconstruction of the final full state took 70 ms and reported 82,988,544 bytes used afterward. The ordinary suite passed 114 tests, the expanded private fixture gate passed 6 tests, and the isolated consumer gate passed 1 test, all without failures, errors or skips. The private tracker inventory now pins 32 records.

No further **new** R0-R2 implementation unit is assigned. The in-flight reconnect unit has reached its tested, documented closeout boundary. R0-R2 is now closed to further discretionary hardening. The user authorized R3 with a separate test/debug app identity so the working tracker installation and data remain untouched. Stop for user participation only when the physical-device validation package and script are ready. HSC-009 Android journal work belongs to R3; R4-R6 remain separate phase decisions and must be surfaced rather than entered through an open-ended hardening loop.

## R2 closeout and R3 authorization gate — owner direction, 2026-10-09

This section supersedes the earlier open-ended instruction to "continue evidence-driven R0-R2 hardening." **Do not abruptly halt work already in progress.** Finish and verify that bounded unit first. No recurring polish loop and no automatic phase transition.

Owner: completed by the active R2 agent; R3 subsequently authorized by the user.

- [x] **Existing in-flight R2 task only:** inspected the branch, local changes, task direction and active checkpoint; identified the reconnect/session-rollover acceptance criteria.
- [x] Finished that bounded unit without starting unrelated R2 enhancements; preserved the four tracker patches, raw-fixture privacy, schema and provenance rules.
- [x] Passed `:core:test`, the complete real-fixture gate and the isolated lifecycle gate after the final changes.
- [x] Reviewed and documented the accepted results and limitations in WORK_LOG and `checkpoints/mobile-fusion-session-continuation.md`.
- [x] **Transition review:** reported the closeout and specific R3 proposal; the user explicitly authorized R3 with a separate test/debug identity.

Stop rule: after safe R2 closeout, **do not initiate further optional/offline hardening** merely to use available time or tokens. Only a concrete, reproducible defect that threatens R2 acceptance may trigger another bounded R2 fix; log its evidence and acceptance criterion. Defer unsupported gameplay semantics, speculative enhancements and QoL ideas to their proper project backlog. If uncertainty is material, ask the user to prioritize rather than invent another task.

Coordination: GitHub and Drive working copies do not update each other automatically. Refresh the remote branch and this TODO before starting/resuming work; reconcile any newer agent changes without overwriting them. This TODO update is a direction for the next safe checkpoint, not a live cancellation or claim that the active agent has read it.

Usage/schedule guard: the prior 02:50 Central 2026-10-09 continuation may be used to finish **the already-started bounded R2 task only**, not to open-endedly polish R2. Retain the previously authorized 02:00–07:00 five-hour ceiling of 95% and the one conditional seven-day reset credit (reread usage afterward; 90% ceiling). Respect hard usage ceilings; if reached, checkpoint recoverably and resume the unfinished unit instead of abruptly abandoning it.

Owner: current Codex session on `feature/mobile-fusion-offline`.

- [x] R3 authorized with a separate test/debug app identity; preserve the working installation and its data.
- [x] R3 checkpoint 1: confirm `com.stroexd.hsdecktracker.debug` isolation; journal completed matches before persistence/export; add an idempotent durable outbox and automated interruption/retry gates.
- [x] R3 checkpoint 2: persist and recover an active match draft without duplicating a completed match.
- [x] R3 checkpoint 3: import selected exporter bundles post-match and persist fusion/outbox status separately from tracker match status.
- [x] R3 checkpoint 4: expose one visible retry/status flow and complete automated storage/permission/interruption gates.
- [x] Alert the user when the R3 test APK and physical-device validation script are ready.
- [x] Historical transition gate satisfied: R4 and later R5 were separately surfaced and explicitly authorized. R6 remains excluded.

Historical state at this checkpoint: R3 source/build work had reached the isolated-phone-validation boundary, while physical-device acceptance was still pending. Later sections record that acceptance and the separately authorized R4 transition; passing local gates alone did not establish either fact.

### R3 device identity correction — 2026-10-09

- [x] Treat the first phone gate as failed safely before installation: Android offered **Update**, and the user cancelled.
- [x] Supersede the `.debug` delivery for this phone without deleting its historical checkpoint.
- [x] Add and independently inspect a dedicated `fusionTest` build identity: package `com.stroexd.hsdecktracker.fusiontest`, version `1.5.0-fusiontest`, label **HS Deck Tracker Fusion Test**.
- [x] Build and stage a corrected v2 APK and phone-validation checklist.
- [x] Device isolation gate: v2 installed as a new app and opened with no historical decks or games from the working tracker; the original tracker retained its active decks and match history.
- [x] Force-stop recovery gate: user played turn 1, stopped during the opponent turn, relaunched and continued; the tracker produced one completed six-turn/6:27 record whose original start duration survived, while the exporter ZIP preserves one incomplete pre-stop fragment and one completed post-relaunch fragment.
- [x] Confirm the completed record/fusion result survives force-stop/reopen without returning to the interrupted active draft.
- [ ] Characterize the missing Turn 2 timeline row. Current evidence indicates a bounded OCR/capture gap: the UI emits only turn groups containing timeline events, while duration and pre-stop Start events persisted.
- [x] Complete phone evidence-import cancellation, successful local fusion and persisted-status checks. Cancellation correctly claims no success; the selected two-fragment ZIP reports **Fused artifact saved locally** and remains saved after reopen.
- [x] Complete R3 physical-device acceptance with the v2 APK. Deterministic automated gates cover permission denial, unreadable input and retry; no additional manufactured phone failure is required.

The `.debug` package was not isolated from this phone's existing tracker installation, regardless of the source repository's production-ID assumption. Only the v2 `.fusiontest` package is active for device validation. R3 device acceptance is complete. Stop and explicitly surface R4 scope/prerequisites; do not begin R4 without user authorization.

### R4 authorized scope — 2026-10-09

The user explicitly authorized R4 after accepting the bounded staged plan below. R4 device builds now use the fresh `com.stroexd.hsdecktracker.fusiontestr4` identity because the accepted R3 debug signing key is unavailable; this preserves both the working tracker and the installed R3 Fusion Test data. Hearthstone locks the user's phone to landscape while open, so do not require an artificial portrait/landscape rotation test during gameplay. Retain dimension/inset-safe coordinate handling and test landscape startup, lock/unlock, capture stop/reconnect, and any fold or window-size change that can occur without leaving landscape.

R4 capture decisions from the owner:

- Reuse and audit the tracker's existing screen-share/OCR module and frame-source lifecycle; do not build a parallel capture stack.
- Give the user an explicit, clearly explained privacy-versus-capability choice among the existing supported capture paths. Prefer Hearthstone-only single-app capture when available, but allow the user to opt into the more compatible/broader existing screen-share path after warning that non-Hearthstone pixels such as notifications may be processed. Visual capture remains opt-in and timestamp-only operation remains available.
- Retain only explicit trigger/manual-bookmark keyframes or crops needed for evidence; do not persist an unrestricted screen recording or unnecessary full-display frames.
- Do not implement a rolling pre-event frame buffer.
- Timestamp bookmarks must continue working when visual capture is unavailable or stopped.
- Do not use an exact battery-percentage threshold as an acceptance gate. The owner will monitor practical battery impact and request reduced demand if needed; still reject obvious thermal escalation, gameplay stutter or runaway capture/storage.

Implementation sequence:

- [x] Owner UX direction implemented with the focused-hand unit: removed the capture radio choices and restored original automatic routing. Existing background Accessibility setting is honored; otherwise normal Android screen-share consent applies. Bounded evidence, lifecycle fixes and isolated identity remain; new records identify the pixel backend without claiming consent scope. Phone validation of v5 remains pending.

- [x] **R4.1 source/build:** audited and extended the existing screen-share/OCR pipeline; added the explicit privacy/capability selector and disclosure, normalized regions, bounded trigger/manual-bookmark keyframes, lifecycle cleanup, and timestamp-only bookmark fallback. No second capture engine or rolling buffer was added.
- [x] **R4.1 phone acceptance:** reviewed privacy-first selection, Compatibility and timestamp-only evidence, manual bookmarks, landscape overlay, sleep/wake continuation, stop/restart with fresh consent, bounded keyframes and post-delete empty export. Recognition after restart has a user-observed delay; seamless match continuity is not claimed.
  - [x] Privacy-first single-app capture becomes available after explicitly selecting Privacy First; the revised always-visible choice UI is preferred over the original dropdown.
  - [x] Deck-tracking overlay persists during the phone test.
  - [x] Source/build fix for the narrow-overlay header: Close remains the final pinned action; Bookmark and Open App moved into a three-dot overflow without shrinking touch targets. Fusion R4 Test APK rebuilt.
  - [x] Phone-verify the v4 narrow-overlay package: user reports all requested checks passed; Minimize, More actions and Close visible; overflow exposes Bookmark/Open App; Bookmark works; Close dismisses normally.
  - [x] Review remaining original R4.1 acceptance evidence: uploaded metadata confirms both policies and timestamp-only bookmarks; user confirms sleep/wake continuation and manual restart with fresh consent; post-delete ZIP contains an empty list and no images. R4.1 accepted.
- [x] **R4.2 bounded closeout:** all three pilot groups retain diagnostic evidence; supported turn/health/mana readers delivered. Owner accepts conservative enrichment rather than implementing every proposed field. Unsupported fields explicitly deferred in `checkpoints/mobile-fusion-r4-closeout.md`.
  - [x] Implement the initial configurable region-probe foundation with experimental positioned OCR attached only to retained trigger/manual keyframes; preserve unknown windows and OCR reuse lineage.
  - [x] Calibrate bounded turn/health/mana geometry against labeled phone frames; defer unsupported hero/card-event semantics rather than generalize broad regions.
  - [x] Add experimental exact turn-affordance, tight health and isolated mana readers; reject reused/unanchored/uncalibrated/merged candidates. Replay labeled saved opening-hand and three gameplay bookmarks with health 26/26/15, preserving unknown mana/opponent health.
  - [x] Validate v5 candidate metadata in Diagnostic (10)/(11), including focused hand and separate counters. v6 fields-3 replay is tested; new v6 phone execution remains pending and is tracked below, not claimed passed.
  - [x] Diagnostic (10) phone metadata observed: seven new saved bookmarks, supported clear-board health 30, exact turn labels, focused-hand occlusion correctly unknown. Minion display/spawn and later Coin history icon captured; Coin animation remains a gap.
  - [x] Focused-hand unit: inferred expanded-hand cue, enlarged-hand/vertical-resource regions, separate untyped adjacent counter and conservative mana-pair reader. Both private archives pass replay; original capture UX restored in the same unit. Opponent health missed by OCR remains unknown.
  - [x] v5 bookmark/backend/layout evidence reviewed and finite R4.3 transition authorized. Settings/no-radio and renewed Accessibility route are not archive-proven; retained as optional future phone observations, not a repeated test campaign or closeout blocker.
  - [x] v5 evidence review: nine new saved bookmarks, all image hashes/sizes verified; fresh expanded-hand, occlusion, counter 8/11/16, opponent health 30/30/15 and reuse/menu guards observed. Capture works. Archive alone does not confirm Settings/no-radio or Accessibility routing.
  - [x] Finite resource unit: landscape-fields-3 uses fully contained separate available/max slots for focused hand and exact same-line pair for normal hand; Diagnostic (11) recovers 1/2, 10/10, 3/10 only. Missing/merged/reused values remain unknown. Ordinary and three private replay gates passed. New APK/device validation is not claimed for this source checkpoint; bundle into the next needed build. Cost-icon classification deferred.
  - [x] Surface bounded R4.3 proposal; owner authorized per-field validation of existing candidates at 90% five-hour ceiling. Unsupported hero/armor/weapon/hand-count/cost-icon readers remain deferred.
- [x] **R4.3 limited closeout:** per-frame assessment policy implemented/tested; owner approves corroborating-only real enrichment. No real accepted observations or authoritative events; independent acceptance is a future evidence gate, not a reason to extend R4 indefinitely.
  - [x] Implement/test pure frame-scoped assessment and explicit external-review references; require distinct human/non-visual sources for accepted, preserve conflicts and unknowns, reject cached OCR/visual descendants/wrong frame and unsupported rules. 141 ordinary tests and three private gates pass. Actual labeled mana assessments are corroborating only; no real accepted observations or automatic tracker-event mutation.
  - [x] Owner chose corroborating-only closeout. No new capture campaign or R5 work to force acceptance. New v6 APK execution remains phone-pending; source/build is not phone acceptance.
- [x] **R4.4:** owner-approved bounded closeout documented; no further discretionary R4 development. Deferred readers and explicit device/acceptance limits recorded; source-ready/tests-passed/APK-built/pushed are separate from phone-verified/merged/deployed.
- [ ] Optional historical follow-up only: if v6 evidence is conveniently returned, record it without reopening general R4 hardening. This is not an active gate or a request for another phone campaign; owner retains installation choice.
- [x] Historical R5 authorization gate satisfied by the later explicit owner authorization and bounded implementation recorded above. Companion fallback, source/privacy/ownership and existing Drive route remain intact; this did not authorize R6.
- [ ] After R4 source and device behavior stabilize—and before any public-release, merge/deployment or broader-distribution planning—remind the owner to consider an optional Codex Security review of MediaProjection boundaries, Android permissions/exported components, URI handling, retained keyframes and cleanup. Do not install or run it without separate authorization, and do not make it a blocker for ordinary R4 work.
- [x] Historical alert/transition requirement satisfied before the separately authorized R5 work.

Historical restart point, now superseded by the authoritative current-state section above: R4 was closed to discretionary development under owner-approved corroborating-only scope. Implementation c866fca / v6 delivery was source/test/build verified; v6 phone execution remained pending at that time. No merge/deployment or automatic phase transition was authorized.
