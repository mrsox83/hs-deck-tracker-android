# Existing retained visual evidence — bounded review

Status: IMAGE-TIME OBSERVATIONS AVAILABLE / DECISION-TIME JOIN NOT ESTABLISHED. Baseline d695dcd; no production edits, tests/build, publication or external writes. Intake4%five-hour/22%weekly under80%; no reset credit used. Previous local docs/helper preserved.

## Evidence inspected

Local .private-r4-review contains nine bookmark JPEGs and a card catalog, no visual-evidence.json manifest. All nine JPEG hashes read; bookmarks5 and7 visually inspected. Pins: bookmark5 SHA2568F8447F308B747B993F2F4CBD784F698D42193CD63E2BDFB78D694E180D55F49; bookmark7 B1DD2856F51B0B43C572DF2AE7EE99F141BA410ECEF3861923261CA64FA9E1BE. These pins identify current extracted bytes, not verified membership in an original diagnostic archive. No player/card/private screen content copied into this report or cloud prompts.

Both inspected images visibly show a lower expanded friendly hand with readable identity text, facedown upper opponent hand, board and resource UI. This supports bounded image-time scene observations, not complete hand inventory, unoccluded slots, particular Power.log instances or knowledge before a later action. Visible clock text has minute-level display precision and does not authenticate date, timezone, source, epoch or decision ordering. No gameplay decisions analyzed.

Read-only searches checked workspace ZIP inventory (excluding toolchain/cache candidates), attachment diagnostic names, Downloads diagnostic names, mounted HSReplay root filenames, Development ZIPs and Older games and files ZIPs. No originating visual diagnostic ZIP/manifest was located in those checked places. This is not proof it does not exist elsewhere or that prior calibration evidence is invalid. WORK_LOG and RealVisualFieldFixtureTest document prior private archives/calibration; those are historical pointers, not a current reopened original bundle. Do not claim their hashes/provenance were reverified in this audit. Do not infer timing from filesystem modification times or bookmark numbering.

## Important refinement to producer feasibility

VisualRegionPilot.kt RegionProbe.observedAt retains OcrFrame.timestamp inside saved keyframe metadata. Thus a complete diagnostic manifest with region probes can distinguish the manual bookmark request time (entry observedAt) from the delivered frame/OCR processing timestamp (probe observedAt). The previous feasibility report's absence of a dedicated VisualKeyframe timestamp must not be read as absence of every frame-related timestamp. This is useful existing metadata; no new field is required just to expose that distinction.

However, neither time is certified pixel acquisition or pre-decision log alignment. ScreenRecognizer reuses prior OCR lines under a new frame timestamp and records regionOcrReused; async fresh OCR may timestamp completion. Missing probes/legacy captures cannot reconstruct those times. Even a reopened manifest would still need independent scene/source/epoch/entity alignment and timing uncertainty to support a decision claim. No such join was established here.

## Disposition and recommendation

Automatic decision-time known-card/private-cost enrichment remains DEFERRED, not a defect declared fixed or an implementation priority. Existing screenshots can be shown as screenshot observations with their own evidence/uncertainty, without retroactively filling hand slots. No new upload or phone capture is required for current position-only work. If the owner later elects to pursue knowledge enrichment, locating the original archive (or an explicitly consented bounded capture protocol) is a prerequisite, not guaranteed proof.

Do not launch another identical marker/metadata audit without a new source or hypothesis. Recommended next owner choice: plan a bounded position-only consumer handoff using the already-tested ordered slots and explicit unknowns, separate from positive knowledge work. This requires a runtime/export integration design and later implementation/APK approval; none is authorized by this report. No schema change, continuous recorder, new buffer, dependencies, canonical closure, commit/push or merge/deployment.
