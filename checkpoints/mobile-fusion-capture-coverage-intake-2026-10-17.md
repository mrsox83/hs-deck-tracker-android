# Mobile Fusion capture-coverage intake — 2026-10-17

## Authority and priority

The owner approved development review and removed the temporary read-only restriction after reviewing the October 17 HS-COACHING report. This authorizes the bounded LOCAL-002 investigation/containment described here. It does not authorize merge or deployment.

Coordinator triage combines report Bugs 1 and 2 because both concern whether screenshot-recognition coverage is absent before journaling or lost later. Development severity is **S1 (major)** and scheduling priority is **P1 (next bounded unit)**: incomplete chronology is exported with a misleading source-level completeness claim and blocks reliable coaching reconstruction. This was compared with the open HSC-004 chronology work and HSC-006/010 companion work; capture provenance comes first because those consumers cannot safely interpret missing actions without it. The report's coaching-impact labels remain preserved as reporter severity, not scheduling authority.

Bug 3 remains within the existing HSC-004 chronology track at **P2**. Current code deliberately clears controller and per-player turn indices when ownership is unresolved rather than guessing. The inspected fusion has 19 PLAY events: 12 with `activeController=2` and 7 unresolved; the report's count of 11 corresponds to ordered-card pairing anchors, not a direct controller count.

Investigation 4 is **closed as expected gameplay semantics, not a defect**. The mounted troubleshooting ZIP's enclosed Power.log independently hashes to the fusion's accepted source `ddc8357c0cd8f87ed37e9d092d758402dd1467a41968ff257c61e1358866b71e`. At all six reported source lines (3280, 5872, 11218, 25020, 30959 and 40820), raw context records `USED_REWIND=1` on entity 83/card `END_000p`, then `BLOCK_START BlockType=GAME_RESET` and `RESET_GAME`. The retained GAME_RESET boundary makes the rewind explicit; this review does not claim broader replay-action deduplication beyond that boundary.

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

## Mounted artifact review

Mounted `<PRIVATE_EVIDENCE_ROOT>` evidence was reviewed read-only after the connector remained unavailable:

- Paladin `46831316-1c89-4c1f-9345-23a0d2a426df`: tracker has 26 events and zero PLAYER_PLAY; accepted fusion has 19 PLAY events and populated raw turns 1–5.
- 20-turn Rogue `a7a239b4-d822-4b8c-91f6-3c9efa49227e`: tracker jumps from turn 1 directly to turn 10, while accepted fusion raw turns 1–11 are continuously populated.
- Warlock `25243bc6-cca8-4139-a13b-ebd703a94d5e`: tracker early turns are sparse, while accepted fusion raw turns 1–11 are continuously populated. Its accepted source is `Hearthstone_2026_10_17_11_02_51/Power.log`, hash `70dc1d7aeb212c12ac3393fb383fe3a3cf8c60bc3f49aa3a7b36f312e171beaa`; the Rogue-session source retained in the artifact is provenance, not the accepted Warlock source.

The owner then uploaded all three exact diagnostic bundles. Their enclosed Power.logs independently hash-match the accepted sources:

- Paladin `Hearthstone_2026_10_17_14_07_45/Power.log`: `3a04ceeb9fdc35b692a5593f53f0416d3953e6fad517493915d769fbc1e58a45`.
- 20-turn Rogue `Hearthstone_2026_10_16_20_33_04/Power.log`: `e775cab2f691796071b05c607e9dbe14eea72f29b4deccf17aa3c2c3f7c9358d`.
- Warlock `Hearthstone_2026_10_17_11_02_51/Power.log`: `70dc1d7aeb212c12ac3393fb383fe3a3cf8c60bc3f49aa3a7b36f312e171beaa`.

Direct streaming comparison confirms continuous raw turn boundaries and PLAY activity where the tracker timeline is empty or sparse. The historical tracker files contain no screenshot-recognition eligibility telemetry, so they cannot retroactively distinguish recognition ineligibility from interruption before journaling. Persisted tracker content and source tracing provide no evidence of completed-export filtering.

## Remaining LOCAL-002 work

- [x] Persist bounded, backward-compatible recognition-coverage intervals in active journals and completed match JSON.
- [x] Distinguish capture gaps, frames observed without play-detection eligibility, and play-detection-eligible intervals without claiming every action was recognized. Explicit recognition stops open a gap; a later observation or match finalization closes it. Gaps longer than 15 seconds are also made explicit. At most 512 intervals are retained, preserving the first 64 and newest 448 while setting a truncation flag.
- [x] Keep coverage metadata out of the gameplay-event return stream; Android applies the side-channel observation after game start and before game finalization. This prevents coverage bookkeeping from changing replay/gameplay event semantics.
- [x] Add synthetic coverage, stop/finalization, long-gap, legacy decode and bounded-retention tests. All 219 ordinary core tests pass across 36 suites; `:app:compileFusionTestKotlin` passes using SDK36. No APK assembly task was run.
- [x] Obtain and hash-verify the three exact accepted-source troubleshooting bundles for the Paladin, 20-turn Rogue and Warlock capture-loss cases.
- Run an owner-approved prospective phone gate on a build containing the coverage instrumentation. Validate that a new completed record distinguishes observed-but-ineligible periods, play-detection-eligible periods and capture gaps without changing gameplay-event replay.
- Do not classify LOCAL-002 as device-accepted, merge or deploy until that phone gate passes.

