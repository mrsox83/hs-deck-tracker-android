# Capture/lifecycle diagnostics — source-local verification

Owner 21:37 UTC approved bounded diagnostic instrumentation/tests/review only. Base a2fdf2ef775aceedc294dc075023691b9afe79d7, feature/mobile-fusion-offline. Five diagnostic source/test files changed; queued three-file note icon remains separate and byte-identical to its reviewed hashes. No product capture-policy fix, new engine, new dependencies, settings changes, APK packaging, commit/push/publication, merge or privacy bypass. Broad HSC-009/010 and existing R4 capture/lifecycle work remain open; PRIORITY: UNASSESSED. Android 17/One UI 9.0 is owner-reported, not an independently verified device build or cause.

## Diagnostic contract

Existing opt-in DiagnosticsRecorder now adds `capture-<random UUID>.jsonl` alongside existing OCR/events/sampled images. Existing ZIP sharing discovers it automatically; frozen artifacts are not changed. Schema `capture-diagnostics/1`: random diagnostic session ID, ordered sequence, wall-clock and elapsed milliseconds, enum kind and bounded numeric fields. No new pixels/images, OCR text, notification content, exception messages, account identifiers or signing material in this metadata. SDK integer is observed on the future device; sourceCode 1 means MediaProjection, 2 accessibility screenshot, 0 unknown. Actual app-only/full-screen selection remains UNKNOWN; no undocumented intent-extra inference.

Recorded observations:

- Start/stop, captured-content visibility and resize callbacks. Visibility is not a per-region readability guarantee; callback absence is unknown.
- Requested/delivered/null frames, delivery elapsed time, raw image timestamp where provided, copy/acquire failures, timeouts and existing attached-surface fallback. Image timestamp domains are retained without claiming universally comparable wall time.
- Unrequested images/no-image/acquire failures are numeric counters flushed on the next capture/timeout/stop, not per-display-frame disk writes. ACQUIRE_FAILED can also contribute to NO_IMAGE; these are not independent failed-frame totals.
- Reader/content/bitmap/physical dimensions, raw screen-coordinate overlay mask and scaled OCR mask. Geometry is observation, not a repaired viewport mapping.
- OCR start/success/failure/reuse, duration, line count, reuse age and exact OcrFrame.timestamp for success/reuse to join existing OCR/image evidence. Duration includes queue/diagnostic overhead, not pure OCR compute time.
- Coarse FRAME_QUALITY statistics reuse the existing 48x27 fingerprint (masked cells excluded): sampled/dark-cell counts and brightness range. These do not prove black rectangles, privacy redaction, unavailable underlying pixels or missed game actions. No quality-based capture/OCR decision is added.

Capture throttling, surface attach/detach, fallback branches, image copying, OCR reuse criteria, mask mapping and game-event/coverage decisions are unchanged. Instrumentation exposes failures without claiming their cause. Source/API callback tests on a real Android device remain unrun.

## Opt-in and overhead bounds

- Recording follows the existing setting live; no new setting. Recorder retained across off/on toggles; all writes remain opt-in. Unique metadata filenames prevent simultaneous recorder instances sharing a metadata file in a same-second directory.
- At most 10,000 records (including terminal LIMIT) and 2 MiB per metadata log. Budget is finite, not a completeness guarantee; LIMIT means later telemetry is unavailable. Each recognition recorder has its own budget, not a new global retention policy. Existing image/frame caps are unchanged (200 images/6000 processed OCR frames).
- At most 24 numeric fields with key length at most 48; no unbounded event queue. One synchronized writer orders handler/worker callbacks. A failed append disables that metadata log after its first exception; caller catches diagnostic failures.
- Enabled writes remain synchronous and can add latency; no phone-performance claim. Off-state still has small map/timing/counter bookkeeping, but no metadata writes or extra quality scan. Enabled quality summaries scan at most 1296 already-computed fingerprint cells; no additional screenshot/capture request or continuous recording.

## Tests and independent review

Final `:core:test :app:compileFusionTestKotlin --offline --no-daemon` PASS in 24s using existing installed SDK/user cache and fresh isolated output. **346 tests / 55 suites, zero failures/errors/skips**, core:test executed. Five new CaptureDiagnosticsTest cases cover reason/order/clocks/serialization, event/byte limits and terminal LIMIT, sequential reopen cap, invalid field bounds, failing sink disabled after first write, and concurrent callbacks with one ordered capped sequence. Final Android Kotlin compilation FROM-CACHE on identical final inputs; prior run compiled final source but failed deleting locked old test-result intermediates. No deletion/process stop used; fresh output resolved it. Earlier iteration also passed before review refinements. Existing compiler/deprecation warnings retained.

Independent read-only `capture_diagnostic_review`: initial findings addressed (opt-in quality gating, high-rate callback I/O, shared-file cap races, failed-write retry and exact OCR timestamp correlation). Final deltas and concurrent/failing-sink tests reviewed, no blockers. This is not Android callback/device acceptance. No Android instrumentation harness/dependency added. Scoped diff whitespace check PASS.

Ignored evidence: `.gradle/capture-diagnostics-20261026/checks.log`, `final-checks.log` (locked-output failure), `verified-checks.log` and `final-output/core/test-results/test/`.

| Reviewed file | SHA256 |
|---|---|
| app/src/main/kotlin/com/stroexd/hsdecktracker/vision/FrameSources.kt | 055d7c5e8c445666397927647d0d2176e50c8d3aac314d05899ffd5144a8d4f1 |
| app/src/main/kotlin/com/stroexd/hsdecktracker/vision/ScreenRecognizer.kt | c9d4be384587b0c61067b055c06848c68f408182c1882ddc96c2a9f605a0c986 |
| app/src/main/kotlin/com/stroexd/hsdecktracker/vision/Diagnostics.kt | 724b2692db08cabbc401a777346a79d510af5ad4abf3ab1b3dff449649684558 |
| core/src/main/kotlin/com/stroexd/hsdecktracker/core/vision/CaptureDiagnostics.kt | c56130e7300ec9f2aadbfc6de3b9b75d1d5cf4949504bd9b98cb267de72f5599 |
| core/src/test/kotlin/com/stroexd/hsdecktracker/core/CaptureDiagnosticsTest.kt | 34b9323a168a643b89d4ec483703e7f294ec8f0aa68131e6f9f08c9b15528df0 |

## Next gates and device plan (not executed)

Next proposed decision: source-only publication/shared reconciliation of reviewed diagnostic patch, preserving queued icon as a separately allowlisted unit; no publication currently authorized. An APK/checks/Drive delivery requires its own approval. No immediate new game/recording/settings action requested by this source-local result.

After a future authorized diagnostic APK: verify exact installed package/build and actual OS/One UI build, owner-confirm capture selection mode. In a noncritical existing Hearthstone/menu context, with existing opt-in diagnostics enabled by owner, correlate a short natural notification/shade/music-control interruption and dismissal with approximate owner timestamp. Compare REQUEST/DELIVERED/image timestamps, callbacks, geometry/masks, OCR success/reuse and existing sampled images. Distinguish missing source pixels, stale delivery, viewport mismatch and OCR rejection; absence of a sampled image cannot prove pixel content. Respect all platform privacy protections; no full-screen fallback forced, no new collection engine/buffer. Android visibility/resize semantics do not guarantee access to obscured pixels. Root cause and Samsung-specific behavior remain UNKNOWN until that evidence exists.

Local TODO/HANDOFF/TASKS/WORK_LOG carry this source-local result, no active implementation/reviewer after closeout. /9 note-export fidelity PASS and remaining preservation checks stay independent. Shared prior diagnostic-plan wording remains pending reconciliation because external publication is outside current authority; source access is available, no write attempted. No coaching/statistics/raw/frozen artifact changes or broad issue closure.
