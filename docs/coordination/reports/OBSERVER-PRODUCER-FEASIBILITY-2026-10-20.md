# Observer evidence producer feasibility

Status: READ-ONLY SOURCE FEASIBILITY COMPLETE. Baseline d695dcd, feature/mobile-fusion-offline. Existing uncommitted contract documentation/helper preserved. No production edits, tests, capture or private fixture replay. Intake2%five-hour/21%weekly under80%; no reset credit used.

## Result

Reuse existing triggered visual evidence for bounded validation; do not build a second recorder, continuous buffer or automatic knowledge engine. Current producer can retain source pixels and OCR-region support, but cannot presently establish exact observer-controller, ordered card-instance or pre-decision log alignment. Defer automatic positive known-at-decision identities/private costs until those proofs exist. This is an evidence limitation, not a claim the human lacked knowledge.

## Existing field map

| Producer | Available | Unsupported claim |
|---|---|---|
| Ocr.kt OcrFrame/OcrLine | Wall-clock timestamp, text boxes, aspect | Exact capture instant, log source/line/epoch, card instance or human perspective |
| ScreenRecognizer.kt process | Current bitmap, content dimensions, reused flag; reused lines get a new frame wall-clock timestamp | Reused OCR is not a fresh observation of current pixels; async OCR completion timestamp is not certified capture time |
| VisionGameTracker.kt detectPlayedCards | Stable expanded-hand name counts after four frames/two seconds; candidate IDs and disappearance events | Ordered slots/duplicate-instance IDs absent; emitted play timing is detection time, not exact action time |
| VisionGameTracker.kt handleOpponent | Turn/region/animation heuristics for opponent popup cards | Complete opponent hand, public audience semantics or selected-observer identity |
| VisualEvidenceRecorder.kt | Explicit/manual-next-frame and named automatic trigger screenshots, JPEG hash/dimensions/bytes, OCR regions/experimental fields/reused flag | Pre-event frame, continuous history or Power.log decision binding |
| VisualEvidence.kt | Match ID, reason, requested mode, capture backend, keyframe hash and status | Requested mode/backend explicitly do not certify granted app/display scope; no verified game controller |
| VisualFieldPilot.kt | Gated experimental turn affordance, hand expansion layout and numeric candidates with OCR support | Card names, slot instances, per-card effective costs or certified decision boundary |

AppContainer.kt onVisualFrame only automatically names game start, turn changes and game end. These triggers do not certify action-before frames. recordVisualBookmark timestamps the request; the recorder attaches the next delivered frame without a separate persisted captured/observed timestamp in VisualKeyframe. Therefore request observedAt must not be treated as pixel time. Multiple queued bookmarks can attach the same delivered frame. These are limits of current evidence semantics, not newly classified high-priority defects.

ScreenRecognizer.kt constructs reused OcrFrame with current wall time and previousLines. VisualEvidenceRecorder records regionOcrReused, and VisualFieldPilot refuses positive candidates when reused. A future join must preserve that distinction rather than interpreting a current image plus older text as a single fresh observation.

VisionGameTracker groups hand names into counts and may resolve several candidate IDs. No slot positions or entity-instance mappings are retained by that path. Sorting OCR boxes alone would not prove game hand order under fan/overlap/expansion. Stats timeline drops frame/region timing even for emitted events. No inference from card-name counts to particular duplicate entities is justified.

## Minimal prospective metadata proposal — not implementation-approved

If an existing-artifact validation establishes the necessary scene semantics, extend the existing evidence path only:

1. Distinguish request time, actual pixel acquisition bounds, OCR observation time/reuse, trigger time and save completion; use monotonic session-relative timing plus wall-clock anchor/uncertainty. Preserve truthful unknowns where the backend lacks capture timing. Timestamp renaming alone cannot supply missing timing.
2. Reference existing capture session, match, keyframe hash and producer/rule version; retain minimal region-level support. Bind game perspective/mode to independently validated visible anchors, never candidate controller alone. An owner annotation may be an explicit annotation, not automatic verification.
3. Only after validation, represent observed card-region identity candidates and slot mapping with ambiguity. Keep per-card cost a separate supported observation, not player mana or printed catalog cost.
4. Use an explicit validated synchronization proof to map a visual observation interval to source/epoch/pre-decision boundary and entity instance. Do not join by nearest wall-clock timestamp, turn parity, card name alone or future action reveal. If uncertainty crosses the action boundary, refuse the claim.

New fields must be optional/versioned and prospective; no backfill of immutable artifacts. Reuse existing keyframes/retention caps (200 files/100MiB) without silently increasing them. Screen/OCR may contain private information: review minimization, deletion/export inclusion and explicit capture consent before storing additional observation content. Do not put raw images/text/accounts in Git/cloud prompts. Extra serialization/storage and timing backend work require separate approval. No dependency necessary merely for the proposed evidence inventory.

## Next proposed bounded step

Read-only inventory/review of already retained visual evidence: determine whether current saved keyframes and manifests provide at least one unambiguous visible scene and an independently supported temporal anchor. Compare only existing matching evidence, keeping raw private contents local. Report supported/unsupported cases and timestamp uncertainty; do not force a log join or request new captures by default. If existing artifacts cannot establish it, return a concrete minimal optional capture protocol for owner choice, or defer knowledge enrichment and continue position-only work. No implementation automatically follows.

Existing264 ordinary/12 private passes are previous source validation, not this feasibility audit. No new code/build/APK, runtime integration, schema/API change, publication, Resource Exchange/canonical mutation, priority assignment or issue closure. This report is local development documentation only.
