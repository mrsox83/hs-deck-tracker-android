# Capture timeout-fallback liveness — source-local verification

## Publication preparation — October31

Owner17:31:50 UTC approved source publication and an update-compatible test APK. The validation narrative below is the earlier source-local receipt, not a claim that publication or delivery has already completed. Publication is restricted to this sanitized checkpoint and the three capture-liveness source/test files. APK preparation retains only the already delivered six-file optional-ZIP overlay in addition to the published fix; queued OCR/HSC-016 changes remain excluded. Remote commit, artifact and delivery verification remain separate gates.

Git stores FrameSources.kt with LF normalization: SHA256 `7365f63b6163b4ec8a211c8b9616263723af2c834b31e1a9f46ec5b7e9b8d843`. The reviewed working-tree byte hash in the table below includes CRLF; normalized text is byte-identical to the staged source. Other two source/test hashes are unchanged.

## Historical source-local receipt

Owner October31 16:27:40 UTC authorized only the confirmed projection timeout-fallback fix, tests and independent review. SOURCE-LOCAL PASS, **not shipped or phone-accepted**. Base/current HEAD81d96b6ceeea84533ecb6a5702914fa3d7d6b90c on feature/mobile-fusion-offline, real index empty. Existing delivered optional-ZIP APK remains unchanged. No commit/push, APK build/install, deployment, live coaching/procedure change, restart/handoff redesign or broader feature work.

Context: read-only diagnosis established a conditional timeout-fallback callback stall. HSC-009/010 capture/lifecycle context remains OPEN; PRIORITY UNASSESSED, no canonical priority assignment or broad missed-ending closure. The stall is source-confirmed, not established as any particular phone incident's cause. Private gameplay evidence is not included in this source handoff.

## Three-file scope and exact hashes

| File | SHA-256 |
| --- | --- |
| app/src/main/kotlin/com/stroexd/hsdecktracker/vision/FrameSources.kt | 7fb17d7561f9c1a2aa54d1f1e76d1f4605c7cdea203c4a7a0b9dc8ee2c8f9198 |
| core/src/main/kotlin/com/stroexd/hsdecktracker/core/vision/PendingCaptureRequest.kt | f557f73f89b8eea58aa2f93dc177523100796348fd8d59f5d794e68466ba36d1 |
| core/src/test/kotlin/com/stroexd/hsdecktracker/core/PendingCaptureRequestTest.kt | e74ce5819d0246fbf5a515262990184bbff3139d813d64335f210a1a7f9c5214 |

`PendingCaptureRequest` is the single pending callback/deadline slot used by the existing MediaProjectionSource, not a second capture engine. Handler-thread confinement is unchanged. Each request owns its deadline token, can receive one bounded fallback grace, and is cleared before callback invocation. Old deadlines cannot complete newer requests; overlaps are explicitly rejected without replacing the old callback. Scheduler refusal/exception, timeout-hook exception and cleanup failure cannot leave an owned unarmed callback.

FrameSources now enters continuous fallback only once and only after successful nonnull surface attachment. It rearms the pending request for one700ms grace interval. A later no-image deadline completes null, permitting existing recognizer scheduling to continue. Continuous-mode timeouts keep the surface attached for later requests; failed attachment leaves future attachment attempts enabled. Normal timeout700ms and fallback threshold three are retained. No new images, recording, retention or pacing policy introduced.

Stop/resize take and cancel pending ownership, clean up platform state, then complete null exactly once. Resize failure closes old/new readers, clears unavailable reader and leaves geometry unknown rather than publishing successful dimensions. Old-reader notifications are ignored. Acquired-image detach/copy failures close the image and complete null; late unrequested-image close is guarded. No WIN/LOSS inference or unfinished-draft save/discard change. Result-confidence, short-game, screen-off/activity/orientation/mask/privacy guards and all queued source files remain unchanged.

## Tests and review actually performed

Final stable `:core:test` **458 tests /64 suites PASS**, zero failures/errors/skips; task executed. Includes **15 new callback-slot tests** within the aggregate, not a claimed separate final filtered run. `:app:compileFusionTestKotlin` PASS FROM-CACHE on matching final inputs. Gradle52s,21 tasks:6 executed/15 cached. Aggregate includes unchanged queued tests; no inference that queued features are shipped. No dependencies/build inputs changed.

Fifteen deterministic virtual-handler cases execute the actual production callback slot: normal frame; no image; bounded fallback with no image; image during grace; late image/no old callback plus next request; cancellation/stop/resize-style take during ordinary/grace wait; stale timeout/new request; overlapping request rejection; scheduler refusal/exception; grace scheduling failure; timeout-hook exception; failed timer removal;20 repeated no-image/fallback cycles; reentrant callback/new request; callback exception leaving no pending slot. No sleeps or duplicate implementation model.

Two earlier checks stopped at compile errors in Kotlin generic inference/nullable-list assertions; explicit types corrected them. Those runs provide **no executed behavioral PASS or failing-before Android reproduction**. Final aggregate has no unresolved test failure. Logs preserved rather than overwritten.

Separate read-only reviewer identified and verified corrections for: callback loss during resize/platform exceptions; callback/image loss during detach exception; and committing continuous-mode flag before successful attachment. Final independent re-review found no remaining blocker, matched all three final hashes and reviewed the15 tests. Reviewer performed no edits/tests/builds/uploads. Android source wiring and platform exception branches were reviewed and compiled, **not exercised by Android mocks/instrumentation/device tests**. The slot tests alone do not prove physical surface delivery, native API behavior or continuous-mode power/performance.

Scoped diff whitespace and known personal-path/privacy marker checks passed; synthetic test values only, no private raw evidence/secrets/source maps in the patch. All six queued HSC-016/OCR and six reviewed ZIP source/test hashes verified unchanged after final checks; unrelated dirty instructions/docs/helpers/private evidence preserved. Source in retained APK snapshot unchanged.

## Limits and next gate

Deadline bounds are cooperative on the existing Handler: ordinarily one700ms wait, at most an additional700ms fallback grace per request. Handler starvation or a blocking Android operation cannot be preempted by this timer. Permanent provider failure may continue producing bounded null captures; this fix guarantees callback progress, not availability of unseen pixels. Consumer exceptions are outside the slot's recovery responsibility, although ownership is cleared before delivery. Physical-device capture/resize/stop races, UI responsiveness and overhead remain unverified.

Result-screen sampling/confidence, recognition restart/result-only blindness, draft replacement/discard risks and recent Power.log source truncation remain separate diagnosed issues. No short-game/confidence relaxation, guessed outcomes or raw/reader-cap change. No broad claim to repair every missed game ending.

Next owner gate: review/source-only publication of this exact patch if desired; separately approve a correctly isolated APK and bounded device checks later. Restart/handoff design remains separately gated. No new game/recording requested in this source-only step. REA/signature/alignment/installation checks NOT RUN / not applicable without a changed APK; their future required gates remain intact.

Local verification receipts retain focused/verified/final logs, XML, exact source hashes and test totals. This sanitized source checkpoint contains no private fixtures, recordings or operator maps. Source review and desktop tests remain distinct from publication, APK/static gates and physical-device acceptance; no broad HSC closure or live intake/procedure change follows from these checks.
