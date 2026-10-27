# Bookmark feedback fix and orientation characterization - 2026-10-26

## Combined source-publication preparation - 2026-10-27

Owner 00:24 UTC approved combined source-only publication with the reviewed source-aware orientation unit and saved-note icon. The newer [orientation checkpoint](mobile-fusion-source-aware-orientation-2026-10-27.md) supersedes earlier capture-helper hashes/policy-only characterization below; remaining reviewed bookmark hashes are unchanged. Public evidence summary omits private diagnostic file/session identifiers and exact device-session timestamps; original detailed receipt remains private. No private recordings/maps or personal machine paths are part of this publication. Earlier source-local/proposed/not-authorized wording below is retained stage history, not the current publication gate. No APK or phone acceptance authorized by this approval.

Owner 23:39 UTC approved synthetic capture-guard regressions and bounded bookmark-status feedback fix/tests/independent review. Base 6b826a0e13144c101af081a9316ace9d622772b5, feature/mobile-fusion-offline. SOURCE-LOCAL PASS only: no commit/push/APK/build packaging/publication/merge/settings or evidence deletion/rotation/cap change. Existing R4 capture/lifecycle/manual-bookmark TODO, shared HSC-009/010 remain OPEN, PRIORITY: UNASSESSED. Shared reconciliation awaits publication authority; no external write attempted, not an access denial.

## Evidence and owner clarification

Owner-supplied MediaProjection diagnostic evidence records sourceCode1 and SDK37; original ZIP identity, checksum and session metadata are retained privately. Exact device APK bytes unverified. Earlier sourceCode2 session is an accessibility baseline. Black/redacted boxes were owner's theory, NOT an observed symptom.

Owner maps the first bookmark to baseline, second to shade start or shortly afterward, third to shade end. These are action associations, not proof of exact display metrics/pixel availability. Owner explicitly reports no visible orientation change: Hearthstone remained foreground landscape and shade came from physical side. Two delivered captures delimit a 19.593-second gap; six intervening ticks reported portrait and skipped requesting frames. This establishes disagreement between visible/captured-app orientation and reported window/display metrics, NOT physical rotation or missing source pixels.

Four manual markers persist in export; all image statuses fail at existing visual store budget. ZIP contains200 keyframes totaling21,326,314 bytes. No new bookmark images; timestamps/status/error survive. Separate sparse diagnostic JPEGs do not sample the exact gap. Raw ZIPs/images/private labels untouched and never copied into public fixtures.

## Bookmark behavior changed

- Feedback follows durable timestamp save: timestamp-only, timestamp saved/image pending, image attached, image failed, full image store, or timestamp-save failed. No unconditional promise that the next frame WILL attach.
- Existing VisualEvidenceRepository implements a thin BookmarkStore interface; actual production BookmarkWorkflow orders timestamp persistence, enqueue and attachment/failure persistence before outcome notification. Schema/export fields unchanged; feedback enum is UI-only.
- UI collects replay-zero transient feedback while overlay exists, using localized English/German messages. Raw exception text is not displayed. Durable detailed status remains in existing exported metadata. Toast/device rendering untested; a terminal toast can be missed if overlay is absent, and no new status-history screen is added.
- Independent review identified stop/restart during asynchronous timestamp persistence: original request could queue after stop drained pending entries, or successor could supply an image. Originating session/capture method now snapshot under start/stop monitor and revalidate+enqueue under same monitor after persistence. New recognition start fails prior queued bookmarks. Existing stop drains pending queue. No capture eligibility/fallback/privacy policy change.
- Cancellation propagates instead of being mislabeled as a storage failure. Timestamp remains preserved if later image/status persistence fails; UI does not claim successful attachment.

## Characterization, not a capture fix

CaptureEligibility.kt is a pure extraction of the current physical/window-metric decision, called from the same ScreenRecognizer gate. It intentionally still returns portrait skip for reported portrait metrics even when independently known shared content is landscape. Seven tests cover this disagreement, unknown geometry, real portrait content, landscape/portrait/landscape transition, screen-off precedence/lazy active predicate, inactive versus active landscape, and current landscape/unknown-content allowance. Stopped early-return remains byte-for-byte unchanged/source-reviewed, not exercised by these core tests.

realDisplaySize on API30+ uses WindowManager.maximumWindowMetrics.bounds via OverlayService context, not an accelerometer/rotation measurement or MediaProjection content dimensions. MediaProjection separately receives onCapturedContentResize and maintains its reader. Code does not log enough context/rotation/configuration detail to choose stale context, platform window metrics or another cause. No source-aware bypass implemented.

Smallest future fix proposal (NOT AUTHORIZED): make eligibility source-aware using independently validated current captured-content geometry for MediaProjection, with explicit unknown/stale/invalid geometry fallback; preserve stopped/screen-off/session/consent/privacy safeguards and accessibility policy. Before implementation agree freshness/transition policy and test portrait reported metrics plus known landscape source, real portrait source, missing/stale geometry, resize transitions, screen-off/stop and source replacement. No blanket guard removal or hidden-pixel assumption. Current synthetic tests expose existing skip; they are not a PASS for desired future behavior.

## Tests and independent review

Final offline :core:test :app:compileFusionTestKotlin PASS in44s using fresh ignored output: **367 tests /58 suites, zero failures/errors/skips**, core tests executed; final Android compilation FROM-CACHE on identical final source. Earlier run compiled this final production source before the test-result lock failure. Existing compiler/deprecation warnings retained.

21 focused tests: CaptureEligibilityTest7, BookmarkFeedbackTest6, BookmarkWorkflowTest8. Actual production workflow tests cover timestamp-write failure/no queue or pending promise; pending only after timestamp write; attached only after image persistence; full-store terminal feedback/timestamp preservation; stop during delayed timestamp persistence; replacement isolation; attachment-write failure; inactive timestamp-only; and cancellation. Repository feedback tests include reopened durable outcomes and adjacent prior-evidence preservation.

Initial pre-workflow implementation passed359 tests/compile in1m6s. Review corrections then encountered locked old Windows test-result output; no files deleted/processes stopped, fresh output used. One subsequent367-test run had a harness failure: it supplied a submitting child scope that completed before deferred image callback. Production uses long-lived appScope; corrected tests use this@runTest. Final367/0 result above supersedes those attempts; no claim of an earlier failing-before executable app/UI test.

Independent bookmark_status_review: initial stop/restart race, cancellation and missing production-order test findings addressed; final re-review and scope-only harness correction no remaining blockers. Reviewer performed no edits/tests. Physical stopped guard and Android toast UI remain source-review/device limits, not fabricated unit/device passes.

## Preservation and paths

Exactly12 scoped production/test/resource files below plus this checkpoint are current task product material. Shared strings contain separate pre-existing has_note additions: removing only five new bookmark entries reproduces both original queued-resource SHA256 hashes exactly. MatchHistoryUi.kt remains byte-identical SHA2563c64a15bc34098b71f42f918e06ebc619f3eb90b0c654f9a2cfb717aa1213d56. All queued icon edits remain uncommitted/unpublished; no inclusion/publication authority inferred. VisualEvidenceRecorder caps200/100MiB, existing images/index, diagnostics and frozen packages unchanged.

The app's existing Settings Delete button beside Share diagnostics calls diagnostic-session clear AND clearVisualEvidence, deleting visual keyframes/index and resetting budget. Deleting an exported ZIP only does NOT reset app-private budget. No deletion performed or requested by this task; preserve evidence. No retention redesign or increased budget.

| Reviewed file | SHA256 |
|---|---|
| app/src/main/kotlin/com/stroexd/hsdecktracker/AppContainer.kt | 479f80b999ed7e9df365e45ba7f9678f6dfcaefcb3eaa48105c45bdc25292292 |
| app/src/main/kotlin/com/stroexd/hsdecktracker/overlay/OverlayContent.kt | ceeaefcf3cebb0f7f93c57a827e72477837fc913032d51583aa9d2f9b9bf9eef |
| app/src/main/kotlin/com/stroexd/hsdecktracker/vision/ScreenRecognizer.kt | dc85ff9c0a9954e90b01bb2da9317129ce42ee430cbdecc43d0b5595f8a0bd12 |
| app/src/main/res/values/strings.xml | 381552aa861af81b571210bf29e0875ac0a5c673a6ec2de06e5188411d8925d8 |
| app/src/main/res/values-de/strings.xml | 50fa20f9cfcf73b888aa092bd328fe6e742c2a869e1ecef79610a17c785fb548 |
| core/src/main/kotlin/com/stroexd/hsdecktracker/core/data/VisualEvidence.kt | a176bc28b520fc4627068ae15815031e371a2e7c4d3b68a4ae1c6d0632b72c41 |
| core/src/main/kotlin/com/stroexd/hsdecktracker/core/data/BookmarkWorkflow.kt | dc17dd6722db5f3f14a78b4dee93c9c2672175a3fd3d00a1d7608f2cec720152 |
| core/src/main/kotlin/com/stroexd/hsdecktracker/core/vision/RecognitionSessionGate.kt | 61fc2ea4385571dad4cf4a2c965c71379c64747530af0e0864e6697219d104f9 |
| core/src/main/kotlin/com/stroexd/hsdecktracker/core/vision/CaptureEligibility.kt | dcc7b1f83f04dcace8a6652b1584c9761199aa825581e9b799e3666026cb5cd2 |
| core/src/test/kotlin/com/stroexd/hsdecktracker/core/BookmarkFeedbackTest.kt | 592496dd1ba4bf702b7f10557813abc031b08a1a5f67b7e140180e3eff2a1111 |
| core/src/test/kotlin/com/stroexd/hsdecktracker/core/BookmarkWorkflowTest.kt | 4e8c5d450eb6d6dac7d0263d360b570b8e8adcaddcc82cc0083411a141639efb |
| core/src/test/kotlin/com/stroexd/hsdecktracker/core/CaptureEligibilityTest.kt | b8ef93c8c24bf74789ebc131acf82ff381d7813053e384040ab27fdd319aaf6d |

Ignored evidence: .gradle/bookmark-status-20261026/checks.log; final-checks.log (locked output); verified-checks.log (test harness failure); final-verified-checks.log and final-verified-output/core/test-results/test; source-hashes.json. Private detailed diagnostic analysis remains ignored, no personal roots/private labels in tests or this checkpoint.

Local TODO/HANDOFF/TASKS/WORK_LOG updated and readback verified at closeout. Reservation released/no active worker after review. Next owner decisions: separately approve source-aware capture fix/design if desired; source publication and any APK remain separately gated. No extra phone recording needed to establish this characterization, no coaching/statistics changes or broad HSC closure.
