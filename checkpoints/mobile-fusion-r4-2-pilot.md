# R4.2 pilot foundation

## Focused-hand and original capture flow — v5

Source-ready: `landscape-pilot-2` has ten bounded regions, including focused hand and vertical resources. `landscape-fields-2` infers expanded hand from large lower-screen card text, withholds occluded friendly health and requires an intact mana pair. An adjacent counter remains untyped; incomplete OCR is unknown. Card-name support is retained without asserting play events. No rolling buffer or new capture loop.

The owner's UX direction supersedes the radio-policy selector: existing enabled Background tracking uses Accessibility; otherwise the original Android screen-share consent flow applies. New evidence records AUTOMATIC_ROUTE and the producing pixel backend, not an unverified app/display consent scope. Legacy settings/evidence remain readable.

Tests-passed: 133 ordinary tests and two required private replay cases across Diagnostic (9)/(10), zero failures/errors. Focused-hand health remains unknown; unobscured health remains 30 in the new clear-board cases. APK and publication details follow in WORK_LOG. Phone-verified is still pending for v5. Next owner gate is two convenient clear/focused-hand bookmarks plus routing confirmation; no repeat of accepted toolbar/lifecycle tests. R4.2 remains open and R4.3/R5 have not begun.

Current bounded unit: experimental exact turn-label and tight health/mana candidates with source OCR support and explicit unknown reasons. User labeled the adjacent 12 in the middle gameplay bookmark as corpse count; merged OCR `1/7 12` remains rejected. Focused-hand capture is a useful next annotation for card-name recovery, not proof of card play. Field promotion remains R4.3 work and has not started.

Verification: 132 ordinary tests and APK assembly passed; `:core:realVisualFieldFixtureTest -PhsVisualFixtureZip=<labeled private ZIP>` passed the saved opening-hand and three gameplay bookmark cases. Friendly health candidates 26/26/15 agree with labeled frame inspection; merged counters and unread opponent health remain null. Build v4 retains R4 package/signer, SHA256 `d650fecdd3842787419796c6f9e6af63233e230fa5e075f513193b455dde7b0a`. No general accuracy percentage or phone acceptance of the new readers is claimed.

Phone v1 evidence: archives (5)/(6) contain only five timestamp bookmarks despite owner-confirmed consent. No region-calibration acceptance. A generation guard addresses a discovered asynchronous recognizer replacement race; source fix is not proof it caused this phone result. Retest one bookmark on v2 and inspect the actual frame/probes before repeating gameplay labeling.

R4.1 accepted from the owner's phone results and uploaded Diagnostic archives, including empty post-delete evidence. Active package remains `com.stroexd.hsdecktracker.fusiontestr4`. Original tracker and R3 data remain isolated.

The pilot attaches eight configurable normalized windows across hero state, turn/resources and event recovery to existing manual/automatic retained frames. It stores at most 32 positioned OCR candidates per window, marks every probe experimental and records OCR reuse. Windows are broad initial calibration hypotheses, not validated hero/health/mana/card recognition. An empty window is unknown. Overlapping windows share one source and cannot corroborate each other. No authoritative events or additional continuous capture are produced.

Next bounded unit: inspect labeled phone keyframes to calibrate these windows, then add individually tested field readers only where evidence supports them. Do not mark R4.2 complete or start R4.3 promotion based on this foundation alone. Capture stop/restart reacquisition delay is observed; seamless match continuity was not proven by that test. The distinct R4 Accessibility label resolves identification in the next APK; do not enable all identically named older services.

Validation/build results are recorded in WORK_LOG. No merge, deployment or R5 transition.

Initial private-frame inspection: the latest manual bookmark in archive (3), 1280 × 547, shows hero portraits/health at center top/bottom, mana/hand lower right and history at approximately x=0.17–0.22. The history window was adjusted to x=0.15–0.25. Card animation heavily obscures the board; this is direct evidence to retain ambiguous/empty probes rather than promote numeric readings. A single frame does not validate region accuracy across devices or layouts.
