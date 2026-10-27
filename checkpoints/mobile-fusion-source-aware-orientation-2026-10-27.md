# Source-aware orientation - bounded local verification, 2026-10-27

## Combined source-publication preparation

Owner 00:24 UTC approved combined source-only publication with reviewed bookmark feedback and saved-note icon. Six final hashes below and all other combined product/test hashes were reverified unchanged; 383-test/Android compilation and independent reviews retained. Public checkpoint summaries omit private diagnostic identifiers/timestamps; original detailed receipts remain private. Earlier no-publication/next-decision wording below is stage history, superseded for source publication only. No APK, phone acceptance, merge or deployment is included.

Owner 23:53 UTC October26 approved source-aware MediaProjection orientation fix/tests/independent review. Base HEAD 6b826a0e13144c101af081a9316ace9d622772b5 / feature/mobile-fusion-offline, plus separately reviewed uncommitted bookmark-feedback unit and queued note icon. No APK, commit, push, publication, merge, settings change or evidence deletion/rotation/cap increase. Existing R4 capture/lifecycle TODO/HSC-009/010 OPEN, PRIORITY UNASSESSED. Shared reconciliation remains pending publication authority; no external write attempted, not access-blocked.

## Evidence and bounded problem

[Bookmark/characterization checkpoint](mobile-fusion-bookmark-feedback-2026-10-26.md) owns prior tests, feedback fix, source hashes and private-evidence references. Owner reports Hearthstone visibly remained foreground landscape throughout shade/music interaction; bookmark1 baseline,2 shade start or shortly after,3 shade end. Reported portrait skips disagreed with visible/captured-app orientation; physical rotation NOT established. Black boxes were a hypothesis, not an observed symptom.

Existing owner-supplied MediaProjection evidence records sourceCode1/SDK37 and captured-content callbacks2520x1080 while six intervening ticks reported portrait and requested no frames, delimiting a 19.593-second delivery gap. Original session identity/timestamps remain private. This establishes gating behavior, not missing source pixels, arbitrary hidden-state access, device APK-byte identity or a universally known platform cause. Raw/frozen evidence untouched; no private image/deck-label fixtures added.

## Changed behavior

Six scoped files below. MediaProjection maintains source-local captured-content geometry only after valid onCapturedContentResize was applied to its reader. Initial display-sized reader does not establish app geometry. Source callback dimensions plus expected reader dimensions must be positive, non-square and orientation-consistent. Start/stop/resize invalidate previous state; stop and late callbacks cannot revive a stopped source.

Geometry uses monotonic elapsed time with10,000ms freshness. Matching newly timestamped actual delivered images renew known geometry; mismatched reader dimensions or backwards time invalidate it. Repeated/missing hardware timestamps cannot refresh it indefinitely. No wall-clock comparison against image timestamp domain is made, only source-local increasing image timestamps.

At capture decision:
- Fresh valid MediaProjection landscape geometry can override unrelated reported portrait window metrics; known source portrait remains blocked even if reported metrics are landscape.
- Unknown/stale geometry falls back to previous reported-display orientation policy; accessibility retains that policy. No mode/consent selection inferred from sourceCode.
- Screen-off, stopped-recognizer/source, active-session and normal pacing/lifecycle guards remain. No extra capture loop, forced sharing mode, settings/privacy change, unconditional portrait bypass or hidden-pixel assumption.
- REQUEST/SKIPPED diagnostic metadata names whether current source geometry was known and which dimensions supplied orientation. Existing opt-in numeric log caps unchanged.

Freshness recovery is deliberately conservative: after long screen-off/inactive/no-delivery interval, if reported metrics remain portrait, stale geometry cannot itself trigger a blind frame probe. A fresh valid source callback or reported-landscape fallback is needed to resume. PreAPI34 sources lacking resize callbacks retain legacy fallback. No seamless restart/recovery guarantee.

## Conservative mask-coordinate boundary

Independent review identified newly allowed frames would otherwise scale overlay-mask coordinates against conflicting reported metrics. Captured-content dimensions cannot prove app-window offsets, full-screen versus app-only consent, or absence of overlays. No guessed transform/mask removal is introduced.

Before MediaProjection mask scaling/fingerprinting/OCR:
- Actual non-landscape/invalid delivered content is not interpreted, including resize races.
- If an overlay mask exists and captured versus reported aspect domains conflict beyond bounded per-axis even-reader rounding, emit explicit numeric SKIPPED/processingSkipped=1/reason4 geometry uncertainty and schedule the next capture.
- No synthetic empty OCR, vision/game event, image/keyframe or falsely reliable observation is emitted for that frame. Numeric REQUEST/DELIVERED evidence continues while enabled.
- Compatible aspect retains the existing mask rule; it does NOT prove arbitrary app-window offset mapping. Existing mapping remains a device/geometry limitation.

Thus this is a source-aware capture-request fix with conservative interpretation, NOT a claim that all interruption recognition gaps are repaired. In the conflicting-mask case, usable OCR/sampled images remain absent by design until coordinate evidence is reliable. Future independent mapping verification is a dependency, not licensed speculation. No new diagnostic image writer/retention policy was added.

## Tests and independent review

Previous bookmark/fallback367-test baseline preserved. ProjectionGeometryTest adds16 focused cases: current landscape versus portrait metrics, accessibility unchanged, unknown fallback/no reader-only bootstrap, real portrait source, expiry/backwards time, matching frame freshness, repeated/missing timestamps, resize transitions, invalid/square/mismatched source geometry, delivered mismatch, stop/new-session/late-callback isolation, screen-off/inactive gates, long-inactive recovery, conflicting/no-mask processing, compatible rounded mapping and delivered portrait race. Existing seven CaptureEligibilityTest cases now explicitly retain fallback/accessibility characterization.

First source-aware iteration:379 tests/Android compile PASS in1m42s. Conservative mask/recovery iteration:383 tests/59 suites/zero failures/errors/skips, Android compile PASS in1m34s. Final rounding refinement overlapped that Gradle invocation; stable-input verification was requested rather than assuming its cache snapshot represented final sources. Final stable verification status is recorded at closeout below. Logs retained, no deletion or process interruption.

Independent bookmark_status_review: initial lifecycle/freshness review found no issue, mask-coordinate concern addressed by fail-closed interpretation; final re-review no remaining blocker. Stale recovery and same-aspect legacy-mapping limits retained explicitly. Reviewer performed no edits/tests. No Android device orientation, consent, mask layout, shade behavior, callback order or performance acceptance inferred from JVM tests/compilation.

## Scope and preservation

| Reviewed file | SHA256 |
|---|---|
| app/src/main/kotlin/com/stroexd/hsdecktracker/vision/FrameSources.kt | 4a59982beec8c926153efd1d9518af27ea24b79cc2c6bb5933f4c1661ed23518 |
| app/src/main/kotlin/com/stroexd/hsdecktracker/vision/ScreenRecognizer.kt | 7d236f748e67b1d4119c68d6b83f519901aa414e9c8a6ff557c20d110a8280a4 |
| core/src/main/kotlin/com/stroexd/hsdecktracker/core/vision/CaptureEligibility.kt | a97e01dca02cabf85257e07a51cf928d28e17ecd1041b0e745b0757749ceca60 |
| core/src/main/kotlin/com/stroexd/hsdecktracker/core/vision/ProjectionGeometry.kt | 9a3ef1d9d059de14a1cb140e7d53114f145946e7606944c348cfb8e27f6c461f |
| core/src/test/kotlin/com/stroexd/hsdecktracker/core/CaptureEligibilityTest.kt | 2f90987eb669a519f20a2ec11a053c88bca166484a140cc2dd6fa1f5733275f0 |
| core/src/test/kotlin/com/stroexd/hsdecktracker/core/ProjectionGeometryTest.kt | 7b3161f7953b04af31a16e16523eb01f80936c1e313454d3574cb63cac0cacaa |

Relative to prior bookmark checkpoint, overlapping ScreenRecognizer.kt/CaptureEligibility.kt/CaptureEligibilityTest.kt hashes are superseded only for this new source-aware scope. Bookmark workflow/repository/session/UI/strings retain reviewed bytes. Note-icon MatchHistoryUi.kt unchanged; English/German prior queued hunks remain byte-preserved, no icon publication/inclusion authorized. Unrelated dirty docs/helpers/private evidence/frozen packages preserved; real index remains empty and HEAD unchanged.

New runtime overhead is O(1) geometry fields/timestamps and arithmetic per existing callback/frame/tick. Resumed capture under valid source geometry uses normal existing pacing rather than three-second inactive polling. No new worker, screenshots beyond permitted existing loop, continuous buffer, engine or disk writer; ordinary opt-in telemetry gains a few numeric fields. Device power/thermal/latency effects unmeasured. Metadata caps and visual-store200/100MiB cap unchanged.

Ignored evidence .gradle/source-orientation-20261026/checks.log, final-checks.log, stable-inputs.log, source-hashes.json and corresponding test-result trees. No raw diagnostic recordings or personal paths/names/deck labels in tests/checkpoint. Shared external issues remain broadly OPEN/unassessed; local checkpoint is not canonical closure.

## Closeout and next gate

Final stable-source verification PASS in 32s, 21 tasks (3 executed/18 up-to-date): 383 tests/59 suites, zero failures/errors/skips, core:test executed; Android compile UP-TO-DATE on stable identical final source. All six recorded hashes unchanged on readback. SOURCE-LOCAL PASS, independent no-blocker review complete. No active worker/reservation or access blocker after closeout; local TODO/HANDOFF/TASKS/WORK_LOG readbacks verified. No APK/commit/push/publication. This bounded result does not certify uninterrupted recognition, device pixel availability or independent mask mapping.

Next possible owner decision after verification: source-only publication of approved reviewed units with an explicit allowlist and preserved unrelated/queued icon edits. APK/delivery/device validation are separate gates. Before claiming actual interruption recognition repair, future device evidence must distinguish source delivery from deliberately skipped interpretation and verify mask-coordinate mapping; no new recording/build requested automatically. No coaching/statistics changes.
