# Mobile Fusion capture-coverage intake — 2026-10-17

## Authority and priority

The owner approved development review and removed the temporary read-only restriction after reviewing the October 17 HS-COACHING report. This authorizes the bounded LOCAL-002 investigation/containment described here. It does not authorize merge or deployment.

Coordinator triage combines report Bugs 1 and 2 because both concern whether screenshot-recognition coverage is absent before journaling or lost later. Development severity is **S1 (major)** and scheduling priority is **P1 (next bounded unit)**: incomplete chronology is exported with a misleading source-level completeness claim and blocks reliable coaching reconstruction. This was compared with the open HSC-004 chronology work and HSC-006/010 companion work; capture provenance comes first because those consumers cannot safely interpret missing actions without it. The report's coaching-impact labels remain preserved as reporter severity, not scheduling authority.

Bug 3 remains within the existing HSC-004 chronology track at **P2**. Current code deliberately clears controller and per-player turn indices when ownership is unresolved rather than guessing. The inspected fusion has 19 PLAY events: 12 with `activeController=2` and 7 unresolved; the report's count of 11 corresponds to ordered-card pairing anchors, not a direct controller count.

Investigation 4 remains **P4 / unclassified observation** until raw event context distinguishes gameplay rewind, reconnect and other reset semantics. Multiple GAME_RESET markers alone are not treated as a defect.

## Evidence and loss boundary

For tracker match `46831316-1c89-4c1f-9345-23a0d2a426df`, the exported tracker record contains 26 timeline events (13 OPPONENT_PLAY, 9 DRAW, 2 EXTRA_DRAW and 2 RETURN) and zero PLAYER_PLAY. The accepted fusion contains 19 Power-derived PLAY events. This establishes missing screenshot-tracker coverage but does not establish that a recognized PLAYER_PLAY was discarded.

Source tracing narrows the current boundary:

1. `VisionGameTracker.detectPlayedCards` emits a friendly play only after a sufficiently large enlarged-hand row remains stable and a previously known card later disappears.
2. `TrackerController` persists the tracker state's timeline into `MatchRecord`; completed-match export serializes that record rather than filtering PLAYER_PLAY.
3. Therefore the inspected absence exists before final export serialization. Existing evidence does not yet distinguish recognition ineligibility from an interruption before journaling.
4. `EvidenceAdapters.tracker` nevertheless marked every finalized tracker JSON source `complete=true`. Match finalization does not prove continuous screenshot-recognition coverage and allowed consumers to mistake absent evidence for inactivity.

## Bounded containment

The tracker fusion source now reports recognition coverage as unknown (`complete=null`) instead of complete. Fusion assembly adds a deterministic diagnostic: match finalization was observed, screenshot-recognition coverage was not measured, and missing tracker events must not be treated as confirmed player inactivity.

This containment is backward-compatible with existing tracker JSON and does not rewrite old artifacts, change pairing, infer actions, or loosen the conservative recognizer. A regression test verifies both the unknown completeness value and the consumer-facing diagnostic.

## Remaining LOCAL-002 work

- Design and persist observable recognition-coverage intervals/gaps with bounded size and backward-compatible serialization.
- Distinguish capture unavailable/interrupted, frames observed but hand row ineligible, and recognizer-active intervals without implying that all actions were recognized.
- Add synthetic lifecycle/restart and eventless-turn tests proving missing evidence differs from confirmed inactivity.
- Recheck the cited October 16–17 artifacts when the mapped Drive surface or connector is available; current H: Drive contents were unavailable during this checkpoint.
- Focused `FusionCoreTest` and the full ordinary core suite pass: 216 tests across 36 suites, zero failures/errors/skips. No APK is warranted until interval instrumentation is implemented and source gates pass.

