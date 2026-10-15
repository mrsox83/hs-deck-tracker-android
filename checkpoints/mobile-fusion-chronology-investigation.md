# HSC-004 chronology investigation — 2026-10-14

Owner authorized investigation/fix with90% five-hour ceiling. Start usage3%/26%weekly; later13%/28%, no reset. Active branch feature/mobile-fusion-offline, baseline ec0e6da, initially clean. Original tracker patches preserved. Latest shared feedback read in prior turn; HSC-004 reported exported-field anomaly, HSC-006/010 compact summary remains dependent. This is HS-FUSION only, not an exporter/coach rewrite or R6 approval.

## Evidence and bounded finding

Affected uploaded fused match2938f2ba / a0c172bbe34acf81b3d9507fdda39f54:290events/476snapshots; playerTurnIndex1 on288events while rawTurn1..7. Accepted source is Power session Hearthstone_2026_10_14_18_40_30, game3, raw SHA25605525573fe6a2386897d74aefe0b068427d51dfc700d60eace75542cd2169143. Eight other source candidates remain provenance, not eight extra games. Existing artifact unchanged.

All root ZIP manifests were searched for that raw hash; no match. Development ZIP inventory contains source archives, not this gameplay capture. Latest available log ZIP instead contains16:09:22 (two games), raw hash55dd3b70955703cfccbaeda14f5e94f5c6b4839375f2833de0a16f26d53cc13c. It is not a substitute for18:40:30. Owner requested to export the retained18:40:30 ZIP; no new game needed. Exact Warrior anomaly cause/repair is NOT yet verified.

Source inspection identified a separate definite isolation defect: EvidenceAdapters built one name->controller map for an entire raw session; later games overwrote earlier same-name mappings, then every game received that session-global map. Controllers/entity IDs are game-local. Synthetic two-game controller-swap fixture reproduces the exposure and verifies the correction. Adapter now retains observations and filters their source lines to each document's own event interval. Reducer applies mappings only once their source line has been reached and the corresponding player entity exists; future observations do not resolve earlier named turn events.

No inferred parity/turn owner or outcome override. Synthetic test verifies unknown before name evidence, known1/2/1/2 controller signals afterwards, per-controller index2 at the end, and no other-game alias provenance. Missing/out-of-range mappings stay unavailable. This confirms a concrete defect class, NOT that it caused the reported last-game anomaly. Raw TURN/CURRENT_PLAYER ordering, unresolved names and controller histories in the exact source still need replay.

## Verification / release boundary

Initial gates196ordinary/3private discovery/6full private fusion tests passed. Added no-future-observation assertion afterwards; final rerun uses separate result paths because Windows locked an old test binary directory. Final result recorded at handoff. Private inputs remain on Drive; only synthetic test/code in Git. No Android/API changes, APK build, phone verification, old-artifact regeneration, merge or deployment. Installed v3 remains current. Before packaging corrected outputs, distinguish the updated fusion/reducer version and verify exact affected replay; current source checkpoint is not a claimed fixed release.

## Next finite gate

Final strengthened gates passed:196ordinary,3private discovery,6full private fusion tests; zero failures/errors/skips. No APK produced. Source checkpoint ready to push, exact affected-game validation still awaits raw source.

Obtain retained18:40:30 ZIP, verify raw SHA256 above, replay game3 and compare raw CURRENT_PLAYER/PLAYER_ID/TURN and actions to derived fields. Add exact asserted expectations; only then close this specific anomaly or implement its additional cause. Preserve original artifact and stable logical identity; no silent overwrite/reclassification. Compact coaching companion work follows verified actor/turn mapping; do not grade actions using the suspect fields. HSC-004 stays INVESTIGATE, HSC-006/010 request not implemented.
