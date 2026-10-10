# Mobile fusion work log

## 2026-10-10 — R4.2 phone capture evidence blocked

The owner supplied four gameplay anchors (turn-one empty board/card play, opponent Coin/hero power; friendly health 30, opponent health drops two). Diagnostic archives (5) and (6), 517 bytes each, contain five identical timestamp-only bookmarks in requested Privacy First policy, no keyframes or region probes. Owner confirms Android consent was shown. Upload succeeds; this is a capture-state evidence gap, not a missing Drive upload. Do not accept pilot calibration from these files.

Source inspection found a concrete replacement race: OverlayService stops the previous recognizer asynchronously, starts its successor, then the previous callback unconditionally clears shared recognition.active and the service reference. Added generation-scoped recognition start/stop and stale-frame suppression so outdated callbacks cannot clear the current session. A regression test exercises old-stop/new-session ordering. This is a candidate cause of the phone symptoms; actual device cause remains unproven until a retained frame is returned. Next phone retest is one bookmark, not another full match sequence. Fresh usage at diagnosis: 27% five-hour, 60% weekly, below owner ceiling 90%; reset unused.

Final core tests and APK assembly passed. v2 APK is 68,212,793 bytes, SHA256 `d22b022130b600d7e697f0c23a5fe03d064390a6c2a4989a93f1af71b9c0edd1`; package and signer match the existing R4 test channel. Local/Drive v2 delivery hash verified. Phone verification pending; R4.2 remains open. No merge/deployment.


## 2026-10-10 — R4.1 accepted; R4.2 diagnostic foundation

Owner authorized proceeding with a 90% five-hour ceiling. Live usage at start: 16% five-hour, 58% seven-day; reset unused. Phone reports establish sleep/wake continuation and stop/restart with fresh Android consent and delayed ongoing-game recognition. Diagnostic uploads in Development/Diagnostic establish Privacy First, Compatibility and timestamp-only metadata. Latest pre-delete archive has 132 keyframes totaling 14,475,869 bytes, maximum width 1280; post-delete archive (4) is 190 bytes containing only `visual-evidence/visual-evidence.json` with `[]`. R4.1 acceptance is complete. These records do not prove seamless match identity recovery or visual semantic accuracy.

R4.2 begins with versioned, configurable broad landscape pilot regions attached only to existing retained trigger/manual keyframes. Each probe stores bounded positioned OCR candidates marked experimental, including empty windows; it never writes match events or interprets text as health/mana/card facts. Region geometry requires device calibration, and overlapping regions remain descendants of the same OCR frame rather than independent evidence. No extra capture loop or continuous retention. The R4 Accessibility service label now explicitly identifies Fusion R4 Test. The owner's reported “video” deletion wording was checked: current source already says “retained visual evidence,” so no speculative wording change was made.

Final `:core:test` (128 tests, zero failures/errors) and `:app:assembleFusionTest` passed. Recurring Windows locks in generated test/resource output required scoped removal of generated binary/app build directories; no source was deleted. APK identity and signer match the installed R4 channel, and packaged resources contain the distinct Accessibility label. APK: 68,212,390 bytes; SHA256 `b495478e204643d4113c39c11ee57166747468473a48aa53de3a12e906c13ac0`. Current usage 22% five-hour / 59% weekly, below 90%; reset unused. Source/tests/APK verified; new pilot phone calibration pending. R4.2 remains incomplete; no R4.3 promotion, merge or deployment.


## 2026-10-10 — remaining R4.1 acceptance review

Owner authorized continuation with an 80% five-hour ceiling. Fresh live usage was 8% five-hour / 57% seven-day; no reset consumed. Shared protocol and active plan were read; shared directory contains no new exchange note. Clean local and independently verified remote tip matched `508e8fa`. No uploaded tracker diagnostics ZIP was found in HSReplay. The latest owner result accepts only the v4 toolbar script. Remaining mode/lifecycle/retained-evidence checks are not established, so the staged R4.2 gate remains open. Added `checkpoints/mobile-fusion-r4-1-remaining-phone-checks.md` with one bounded completion script using the installed v4 package; no rebuild or repeated toolbar test is needed. No implementation, merge or deployment performed.


## 2026-10-10 — v4 toolbar phone acceptance

The owner reports every requested v4 layout retest passed: visible Minimize/More actions/Close, Bookmark and Open app in overflow, successful bookmark feedback, and Close dismissal. This accepts the narrow-toolbar fix at source commit `4b881be`. No additional build or test was required for this documentation-only checkpoint. Original R4.1 lifecycle/mode/retained-evidence checks remain subject to evidence review; this layout-only report does not establish those results. R4.2 remains unstarted. Live usage: five-hour 7%, seven-day 56%; reset credit unused. No merge or deployment.


## 2026-10-07 — preflight

User approved continuation of R0–R2 using the attached implementation plan. The attachment's stale approval-pending wording is not a new approval requirement. No implementation changes have been made.

Shell execution succeeded. Initial checkout was `work` at `de24d36d48d3cddc4162f966d60f9918faec7611`. Remote lookup and fetch verified `feature/mobile-fusion-offline` at `d3bc0fb15c6cf4e29ba75fc0aac54b1ee75541bd`; the local feature branch now points there. Its four patches include inferred friendly plays, hand-gap recovery, OCR feedback-loop correction, and manual match-export recovery.

GitHub API repository access returned Forbidden; `gh auth status` reports the configured GH_TOKEN is invalid. Git read access works. `git push --dry-run` against the existing exact branch head returned Everything up-to-date, which does not establish new-commit write authorization. No remote refs were changed.

The first core-test invocation failed because the default Gradle cache under `/home/agent` is unwritable. Using `GRADLE_USER_HOME=/workspace/.gradle` fixed the cache location. Java then failed direct DNS resolution for services.gradle.org. Supplying JVM HTTP/HTTPS proxy properties for the inherited session proxy allowed Gradle 8.14.3 to download successfully. The Android plugin also needed `ANDROID_USER_HOME=/workspace/.android` to avoid the read-only default Android home. With both writable cache locations and proxy settings, dependency downloads, core compilation, and `:core:test` passed (BUILD SUCCESSFUL). Nonfatal fontconfig/ONNX cache warnings occurred. This validates existing tests, not the unavailable real fusion fixtures.

No configured secrets or outbound identities were reported by the cloud runtime. Access to the plan's Drive exporter source URL was denied by the proxy (CONNECT 403). Required private source/fixture artifacts are absent locally. No attempt was made to bypass the network policy; factual summaries in the plan are not substitutes for raw fixtures or independent labels.

Only planning/checkpoint documentation was added. No app/core implementation, merge, or deployment was performed.

Baseline test report totals: tests=94, failures=0, errors=0, skipped=0. Existing diagnostic tests may return early without external fixtures; a green baseline does not establish fixture coverage.

## 2026-10-07 — R0 complete, R1/R2 core started

Applied the user-supplied checkpoint patch on `feature/mobile-fusion-offline` after independently verifying the remote branch remained at `d3bc0fb15c6cf4e29ba75fc0aac54b1ee75541bd`. The checkpoint commit is local only. No merge, push or deployment was performed.

Authorized access to `H:\My Drive\HSReplay` is now available. Inspected the v0.4 exporter source, all current mobile export bundles and tracker JSON fixtures, and independently read the representative Rafaam pair. Exact inventory hashes and source/build uncertainty are in `checkpoints/mobile-fusion-r0.md`. Raw private artifacts were not copied into the repository.

Added a pure-JVM `hs-fused-match/1` core: immutable provenance-bearing claims and sources, tracker and Power evidence adapters, deterministic replay deduplication, conservative pairing with ambiguous/composite rejection, ordered nested action blocks, time-scoped entity/tag history, unknown-tag retention, separate choice stages, boundary snapshots, and compact source pointers. This is offline-only and does not change tracker capture, persistence, legacy export, Android permissions or UI.

Added five synthetic fusion tests covering nested ordering/provenance, unknown tags and incomplete blocks, accepted and ambiguous pairing, shuffled/repeated import, and composite rejection. The complete `:core:test` suite passed using JDK 17 and cached dependencies. Real-fixture acceptance remains open; synthetic success is not a substitute for the private-fixture assertions requested by R1/R2.

## 2026-10-08 — first real-fixture gate

Added `PowerClock` to resolve exporter log times only when a dated session alias and explicit timezone are available, including guarded midnight rollover. Added a separate `realFusionFixtureTest` Gradle task that requires `-PhsFusionFixtureDir`; the ordinary `:core:test` task excludes it, so CI neither requires private data nor reports a false fixture pass.

The dedicated gate passed against the exact Rafaam pair on Drive. It verified the tracker ZIP/document hashes, bundle schema, inner Power.log manifest hash, 22,601 selected events, nine distinct choices, submitted and confirmed Raze/Enthrall stages, Master Dusk at source line 38189 before Deathwing at 45191, explicit target zero represented as no target, seven-second endpoint difference after America/Chicago clock resolution, accepted multi-characteristic pairing, replay deduplication and evidence pointers on canonical events and tag histories.

The ordinary private-fixture-free `:core:test` gate also passed after the change. No private artifacts were copied into Git. No capture, persistence, UI, Android permission, merge, push or deployment changes were made.

Extended the real gate to the legacy `hs-export-bundle/0.3` truncated multi-match fixture. The bundle adapter now inspects raw Power.log for Hearthstone's size-limit marker even when old metadata omits `source_truncated`, records the marker line, and applies truncation only to the final source match. The real fixture verified three matches: two completed/non-truncated records and one incomplete/truncated suffix, all retaining partial events and a last-valid snapshot. Synthetic coverage verifies that earlier matches in the same session are not mislabeled truncated. Both the two-test private gate and ordinary core suite passed.

Corrected Power source identity to use the inner Power.log content hash plus source match index rather than the ZIP wrapper hash. Different physical wrappers remain as artifact aliases but no longer become independent witnesses. Synthetic wrapper-order/alias tests and the two real duplicate Drive exports passed. With versioned schema/adapters, stable identities, conservative pairing, deterministic replay deduplication, compact provenance output, shuffled/repeated import, ambiguous/composite rejection and partial-result coverage now asserted, the scoped R1 checklist is complete. R2 remains open.

## 2026-10-08 — R2 reducer checkpoint

Added time-scoped card identity revisions so later SHOW/CHANGE evidence does not rewrite when an earlier identity became known. Snapshot construction now records a pre-action boundary before each outer block and a post-resolution boundary only after the outer block closes; nested block ends no longer masquerade as settled action boundaries. Unterminated stacks still mark the last-valid snapshot unresolved.

Added typed, provenance-bearing observed counters for resources, resource use, temporary mana, overload, health, damage, armor, Herald, quest progress and turns-in-play while retaining every unknown raw tag separately. No remaining-health, lethal or intent inference is made from incomplete operands. Synthetic tests assert identity timing, nested outer-boundary behavior and exact Herald evidence. The real Rafaam gate confirms multiple identity revisions plus observed RESOURCES_USED and HERALD_COLOSSAL_AMOUNT counters. Ordinary and private gates passed. R2 is not complete.

Added typed zone, position, controller and visibility histories plus observed entity-reference links such as CREATOR, HERO_ENTITY and HERO_POWER. HIDE_ENTITY records visibility only and is never interpreted as death. Added per-controller turn indices that advance on resolved CURRENT_PLAYER transitions without relying on odd/even raw-turn parity.

Fixing name-only player tags exposed and removed an unsafe behavior: unresolved names no longer inherit the last pending entity. The bundle adapter now extracts `PlayerID, PlayerName` mappings from raw Power.log with exact source lines, and the reducer joins those mappings to selected-stream Player entities. Alias history retains the raw mapping evidence. The real fixture proves typed resource/Herald counters, hero links, visibility and zone histories, and canonical actions with resolved controller/turn indices. Ordinary and private gates passed.

Added typed player snapshots joined through observed HERO_ENTITY relationships. HEALTH, DAMAGE and ARMOR are read only from the currently linked hero instance. Remaining health is derived only when both HEALTH and DAMAGE are present, cites both observations and does not include armor. Resource, resource-used, temporary-resource and overload fields remain separate observed claims; no available-mana formula is inferred. Synthetic tests assert exact operands and evidence lines, and the real fixture confirms at least one current-hero health derivation with two retained evidence references. Both gates passed.

Added separate typed quest and Herald state. Quest progress, total, explicit completion and reward entity are independent claims; equal progress and total do not imply completion. Herald amount and class are observed per player, while threshold status remains unknown until a game-build-specific interpretation is configured. Synthetic tests cover progress equality without completion and separate reward evidence. The real fixture confirms observed Herald amount while threshold interpretation remains unknown. The first R2 implementation checklist item is complete; critical-fact breadth and remaining synthetic edge cases stay open.

## 2026-10-08 — R2 exact-provenance gate

Made pairing evidence explicit rather than treating matching values as self-proving. Endpoint, result, first-player and card-anchor agreements now contribute only when the Power side has an exact source-line reference; the tracker side contributes the corresponding JSON pointer. Matching values without Power evidence remain pending, and the accepted pairing candidate retains the complete evidence set.

Added a fused-artifact provenance validator. It rejects accepted pairings without an evidenced candidate, non-unknown typed claims without evidence, evidence without an exact line or JSON pointer, references to sources absent from the fused source catalog, and unprovenanced canonical events, entity histories, choice stages or snapshots. Synthetic tests exercise both a valid artifact and deliberate missing/unknown-source failures.

The Rafaam gate now derives pairing facts from reduced Power evidence for local controller 2 instead of supplying hand-authored result and first-player values. The assembled real fused artifact passes the full provenance validator. The ordinary suite passed 107 tests and the private fixture gate passed 3 tests, with zero failures, errors or skips. No private fixture was copied into Git, and no tracker capture, Android integration, merge, push or deployment was performed. This completes the approved offline R0-R2 implementation checklist; remote write authentication remains separately unproven.

## 2026-10-08 — final R0-R2 compatibility audit

Audited the completed branch against tracker baseline `d3bc0fb`. The four tracker patches remain intact, and the later change range is limited to fusion core/tests, the dedicated Gradle fixture task and checkpoint documentation. Added an explicit `hs-fused-match/1` JSON encode/decode equality test that revalidates provenance after decoding. Added a final audit record and a five-hour-first Codex usage plan with stabilization thresholds, checkpoint rules and explicit handling for push blockers and reset authorization. The ordinary suite passed 108 tests and the private fixture gate passed 3 tests with no failures, errors or skips.

## 2026-10-08 — strict fused-artifact codec

Added `FusionArtifactCodec` as the supported `hs-fused-match/1` boundary. Imports now require an explicit schema, reject unsupported or non-string schemas and malformed JSON, and validate all provenance after decoding. Exports reject unsupported schemas or invalid provenance before writing. Synthetic tests cover String and stream round trips plus missing/wrong schema, malformed JSON and invalid evidence; the ordinary suite now passes 110 tests.

The first real-fixture round trip exposed two memory limits under the default test heap. Materializing the full artifact as a String exhausted the JSON writer; streaming removed that duplicate buffer. Decoding the stream while the test still retained the original full fused graph then exhausted the heap by allocating a second graph. The final real gate therefore proves that the 22,601-event artifact validates and streams completely to disk without raising heap limits, while full stream decode equality remains covered on the synthetic artifact. Large-artifact decoding in an isolated lifecycle remains explicitly unmeasured rather than being claimed from the failed duplicate-graph test. All 3 private tests pass. No Android integration, merge or deployment was performed.

## 2026-10-08 — paused-window handoff

Paused at the user's request after verifying clean local and remote branch tips at `239ee23482217e2b068ea2a06658f1c2c1dfc236`. Real Git pushes succeeded, superseding the initial `gh auth`/API uncertainty; the TODO now records write authentication as proven. The latest gates remain 110 ordinary tests and 3 private fixture tests with zero failures, errors or skips.

Assigned the next bounded R0-R2 hardening unit to a future Codex window: measure full real-artifact stream decoding in an isolated lifecycle, record size and memory conditions, and characterize dominant sections only if decoding remains heavy. R3-R6 work remains unassigned pending explicit user authorization. See `checkpoints/mobile-fusion-pause-handoff.md` for exact restart commands and boundaries.

## 2026-10-08 — isolated lifecycle and sparse snapshots

Added separate producer and consumer Gradle test workers with explicit 512 MiB heaps. The producer writes the exact Rafaam fused artifact to disk; the consumer starts fresh, stream-decodes it and records artifact size, runtime and heap conditions. The first isolated artifact was 139,607,285 bytes and still exhausted the consumer heap during `EvidenceRef` deserialization, proving that retaining the reducer graph was not the sole cause.

Byte-range profiling found snapshots occupied 133,497,716 bytes (95.62%): repeated entity tags to counters accounted for 80,082,092 bytes, and counters to players accounted for 44,420,330 bytes. This contradicted the approved plan's requirement to store sparse deltas plus useful checkpoints rather than duplicate every entity after each low-level tag.

Added schema-compatible `FULL`/`DELTA` snapshot state, full checkpoints at the first, turn-signal and last-valid boundaries, nested entity/counter deltas, player/quest deltas, and a lazy full-state materializer. Missing `stateMode` defaults to `FULL`, preserving legacy artifact interpretation. Reducer state is monotonic for these maps, so the delta format does not require removal markers in R2.

The same fused artifact now measures 11,033,645 bytes, a 92.1% reduction. Its snapshot section fell to 4,924,095 bytes while event, entity and choice section sizes remained unchanged. A fresh 512 MiB consumer decoded all 1,144 snapshots in 540 ms and reported 71,857,664 bytes used after decode; lazy reconstruction of the final full state took 78 ms and reported 81,294,848 bytes used afterward. Final gates: 112 ordinary tests, 3 private fixture tests and 1 isolated lifecycle test, all with zero failures, errors or skips. No private fixture entered Git; no Android integration, merge or deployment was performed.

A focused consumer-safety review added a leading-delta guard to both provenance validation and lazy materialization. Malformed compact streams can no longer reconstruct silently from empty state, while empty snapshot lists and legacy all-full artifacts remain valid. The ordinary gate now passes 112 tests.

## 2026-10-08 — full private corpus smoke gate

Refreshed the Drive inventory from 19 to 29 tracker records and from 11 to 21 exporter bundles, pinning exact SHA-256 values for every current fixture. Added sequential corpus tests that parse every tracker and reduce all 48 exporter source matches under the existing 512 MiB private worker without assigning speculative cross-source pairings.

The full exporter corpus produced 21,018 canonical events and 29,674 compact snapshots. Every canonical event retained exact same-source line evidence; every nonempty snapshot stream began and ended with `FULL` state; lazy materialization reproduced each final checkpoint. The expanded private gate passes 5 tests with zero failures, errors or skips. Raw fixtures remain outside Git, and no Android integration, merge or deployment was performed.

Pinned the corpus distribution and warning profile: 4 schema-0.2, 2 schema-0.3 and 15 schema-0.4 bundles; 43 completed matches; 4 explicitly truncated suffixes; and one incomplete non-truncated suffix with two open blocks. Six unmatched block ends trace to the same selected inner source repeated through three wrappers. Direct source-event balance inspection confirmed that corresponding outer starts are absent from the selected evidence, so the diagnostics remain visible rather than being suppressed or guessed away.

## 2026-10-09 — scheduled usage handoff

At 01:30 Central, live usage was 78% of the active five-hour window and 93% of the seven-day window. The branch was clean and synchronized at `00186342bf921784fd16334bbc55a76c5774ea98`. No new implementation was started in the approximately 2% remaining before the normal 80% ceiling.

Continuation is scheduled for 02:50 Central after the five-hour reset. Between 02:00 and 07:00 Central, the authorized five-hour ceiling is 95%. If the seven-day limit actually blocks work, the user explicitly authorized one full reset credit; after that reset, usage must be reread and the ceiling becomes 90% of the resulting five-hour window. Further reset credits require new authorization. Scope remains offline R0-R2 only, with completed checkpoints committed and pushed; no merge or deployment.

## 2026-10-09 — reconnect/session-rollover fusion

Read the Shared Exchange protocol, canonical feedback backlog and active HS-FUSION plan before coding. Linked the work to HSC-004, HSC-009 and HSC-010 without changing other projects' plans. At the scheduled start, live usage was 10% of the five-hour window and 97% of the seven-day window; during validation it was 25% / 99%. The user then manually consumed one full reset. The verified post-reset values were 1% / 0%, and one reset credit remained.

Implemented conservative offline stitching for the verified reconnect case. Session `Hearthstone_2026_10_07_22_44_20` ends incomplete/non-truncated with Deathwing choice 16 open; session `Hearthstone_2026_10_07_23_04_47` resumes the same entity/options, confirms Enthrall and reaches evidenced FINAL_GAMEOVER. Acceptance also requires bounded clocks, tracker endpoint agreement, both player/controller identities, at least three stable entity identities and raw-turn continuity. The derived source retains both parents and exact provenance, and the artifact records the source-session gap rather than pretending capture was continuous.

The paired tracker fixture is the 26-turn LOSS versus Death Knight record ending `20261008T041335265Z`; the similarly named `20261007T224522751Z` record is a separate 13-turn WIN versus Priest and was not used. Duplicate wrappers for the later session still collapse through the existing inner-source identity.

Refreshed the private tracker inventory from 29 to 32 records. Final combined gates passed: 114 ordinary tests, 6 private tests and 1 isolated lifecycle test, with zero failures, errors or skips. The isolated artifact measured 11,033,664 bytes; decode took 532 ms using 73,551,360 bytes after decode under a 536,870,912-byte maximum heap, and final-state materialization took 70 ms using 82,988,544 bytes afterward. Private evidence remains outside Git. No Android integration, merge or deployment was performed.

The user then authorized R3 with a separate test/debug app identity, specifically preserving the working tracker installation and data. Codex will alert the user only when physical-device validation is ready and will explicitly surface the R4-R6 phase boundaries instead of continuing indefinite hardening.

## 2026-10-09 — R3 checkpoint 1: completed-match journal/outbox

Confirmed that the existing debug build already uses application ID `com.stroexd.hsdecktracker.debug`, version `1.5.0-debug`, a debug signature and a package-scoped file provider. It installs beside `com.stroexd.hsdecktracker` and receives separate app-private storage, satisfying the user's isolation choice without changing the production identity.

Closed the crash gap between `matches.add(record)` and best-effort SAF export. A completed match is now durably staged in `match-outbox.json` before idempotent match persistence. The journal distinguishes STAGED, LOCALLY_COMMITTED, TRANSFER_PENDING, LOCALLY_EXPORTED, TRANSFER_FAILED and REMOTELY_VERIFIED; the current folder exporter never claims REMOTELY_VERIFIED. Startup recovery replays nonterminal entries, duplicate match IDs with different content are rejected, cancellation leaves a pending retry, and transfer failures retain attempts and the last error.

Manual single-match and bulk recovery now use the same serialized committer, preserving WRITTEN versus ALREADY_PRESENT behavior and updating journal state. Automated tests simulate interruption before local commit, after commit/before status advancement, a pending transfer at restart, failed folder access, retry and duplicate suppression.

Validation: 116 core tests passed with zero failures/errors/skips; the debug Kotlin variant compiled; and `:app:assembleDebug` succeeded after the final manual-recheck correction. The resulting 68,778,914-byte APK has SHA-256 `77bce0caed7e2046178749f4eb402f6fd7e0cbd87d768f737d5615eeaf705537`; packaged metadata and manifest both identify `com.stroexd.hsdecktracker.debug` and `android:debuggable=true`. This is a source/build checkpoint, not phone verification, and the user has not yet been asked to install it.

Remaining R3 work is deliberately bounded: active-match draft recovery, post-match exporter-bundle import/fusion persistence, one visible status/retry flow, then automated failure gates and a scripted phone test. No R4 capture/bookmark, R5 Shizuku integration, R6 Drive integration, merge or deployment was started.

## 2026-10-09 — R3 checkpoint 2: active-match draft recovery

Added a versioned-on-disk active tracker journal at `active-match.json`. Every accepted tracker state transition now receives a stable draft ID and enters one serialized command stream. On restart, a nonterminal draft is restored with its deck, counters, opponent evidence, turn and timeline intact. Completing the game reuses that draft ID as the `MatchRecord` ID, stages and persists it through the completed-match outbox, and only then clears the matching active draft.

The restart gate rejects a stale active draft whenever the same ID already exists in either `matches.json` or `match-outbox.json`. This covers a process interruption after completed-match staging but before active-journal cleanup without creating a second match. Cleanup is ID-conditional, so a late command for an older match cannot erase a newer active game. Explicit tracker stop clears its draft, while the inactive post-completion reset is deliberately not journaled ahead of durable commit.

Automated coverage verifies journal encode/reload, wrong-ID cleanup protection, restored state identity, ordered tracker transitions and stable completion identity. The ordinary core suite passes 118 tests with zero failures/errors/skips, and `:app:assembleDebug` succeeds. The resulting isolated APK is 68,959,268 bytes with SHA-256 `75088765ccf63f76661b8e60d6e2a919288358919ab7933347d8a7b9e595c5a7`; it remains package `com.stroexd.hsdecktracker.debug` with separate app-private data by the already-verified debug variant configuration.

This checkpoint proves source behavior, persistence tests and buildability, not physical-device process-death recovery. The user should not install yet: checkpoint 3 post-match bundle ingestion and checkpoint 4 visible retry/status plus the scripted phone validation gate remain. R4 has not started and must be surfaced explicitly after R3 acceptance.

## 2026-10-09 — R3 checkpoint 3: selected post-match evidence import

Added an explicit multi-document action on a saved match for selecting one or more exporter ZIP bundles. Parsing, reduction, conservative continuation evaluation, controller-aware pairing, provenance validation and artifact encoding run off the UI thread. The importer accepts only one uniquely evidenced Power source; ambiguous, unmatched and malformed selections remain retryable and never rewrite the tracker `MatchRecord`.

Fusion status is stored separately in `fusion-outbox.json` with RECEIVED, PROCESSING, PENDING_PAIRING, FUSED_LOCAL and FAILED states, attempt count, selected bundle hashes, pairing evidence, error text and the local artifact filename. Accepted artifacts are streamed through `FusionArtifactCodec` to an atomic app-private `fused-matches/<logical-id>.json` replacement. FUSED_LOCAL means only that the provenance-validated artifact exists in the debug app's private storage; it does not claim export or remote verification.

Synthetic end-to-end tests cover a uniquely matched selected bundle, persisted and decodable fused output, a pending conservative pairing, malformed input, retry attempt accounting and reloading status from disk. The ordinary suite passes 120 tests. The six-test private fixture gate still passes against `H:\My Drive\HSReplay`, including all current pinned tracker/exporter evidence and reconnect continuation. The final debug APK assembles successfully at 69,962,773 bytes with SHA-256 `a313b414870030978027b6ee1c447477e42a03cfcfcb84a9367e453b2e7125d5`.

Live five-hour usage was 52% when this checkpoint began and 74% after implementation, ordinary tests, Android build and private-fixture validation. The next bounded unit is checkpoint 4 only: visible persisted status/retry, interruption/permission gates, and the phone-validation script. The user has not yet been asked to install. R4 remains unstarted and will be surfaced explicitly after R3 device acceptance.

## 2026-10-09 — R3 checkpoint 4: visible recovery and device package

Added persisted fusion status to the saved-match detail screen. RECEIVED, PROCESSING, PENDING_PAIRING, FUSED_LOCAL and FAILED have distinct user-facing states; pending and failed imports offer **Select bundle again**. Restart converts abandoned RECEIVED/PROCESSING work to a visible retryable failure before a new import can begin. File-picker access failures are also retained without degrading an already completed FUSED_LOCAL artifact.

Added deterministic gates for interrupted-status recovery, permission denial, missing document handles and atomic artifact replacement. A failed encode removes its `.tmp` staging file and leaves the prior valid fused artifact byte-for-byte intact. Import cancellation remains cancellation rather than being recorded as success.

Final validation passed 123 ordinary core tests and 6 private-fixture tests. `:app:assembleDebug` passed. Packaged metadata independently reports `com.stroexd.hsdecktracker.debug`, version `1.5.0-debug` and `application-debuggable`. The 68,824,434-byte APK has SHA-256 `321ea959e0a08b5a10dfd58e80bf3c3ca92f28950f5a024005e13c3dbe77a361`.

The ready-to-test package is staged locally at `C:\Users\Xs_da\Documents\SoxCoachExporter\Matches\mobile-fusion-r3-device-validation`. An attempted copy to `H:\My Drive\HSReplay\Development\mobile-fusion-r3-device-validation` was rejected by the environment's external-sync safeguard because this turn did not explicitly authorize that upload; no Drive destination was created or modified. The local package contains the APK, checksum and numbered phone-validation README.

Five-hour usage began at 0%, was 28% after implementation, automated gates, private fixtures and APK packaging, and 32% at the final pre-commit check—below the user's 60% ceiling. R4 was not started. R3 is source-ready/tests-passed/APK-built, but not yet phone-verified, merged or deployed.

The final live post-push usage reading was 36% of the five-hour window. The published checkpoint remained below the 60% ceiling.

The user subsequently explicitly authorized the external synced-Drive copy. The APK, checksum and phone-validation README were copied to `H:\My Drive\HSReplay\Development\mobile-fusion-r3-device-validation`; the copied 68,824,434-byte APK was reread from Drive and its SHA-256 matched `321ea959e0a08b5a10dfd58e80bf3c3ca92f28950f5a024005e13c3dbe77a361`.

## 2026-10-09 — R3 device identity correction

The first physical-device isolation gate stopped safely before installation. Android offered **Update** rather than **Install** for the `.debug` APK, and the user cancelled. This proves the existing working installation on this phone already occupies `com.stroexd.hsdecktracker.debug`; the earlier repository-based assumption that `.debug` would be separate from the phone installation was invalid for this device. No install, data clear, uninstall, merge or deployment occurred.

Added a dedicated `fusionTest` Android build type derived from debug with application-ID suffix `.fusiontest`, version suffix `-fusiontest`, and the distinct visible label **HS Deck Tracker Fusion Test**. `:app:assembleFusionTest` passed. Independent packaged-metadata inspection reports package `com.stroexd.hsdecktracker.fusiontest`, version `1.5.0-fusiontest`, `application-debuggable`, and package-scoped provider/permission names.

The final cleanly rebuilt APK is 68,036,586 bytes with SHA-256 `9744f913f7c40aaf7e4cbc1e2833a95cccb12e6eaf626f72be8df5a5ab8805d2`. The original v1 local and Drive deliveries are preserved as superseded historical artifacts; the active v2 delivery uses new `mobile-fusion-r3-device-validation-v2` directories so nothing is overwritten. Physical-device behavior remains pending. R4 has not started.

Live five-hour usage was 34% during the corrective checkpoint, below the user-authorized 60% ceiling.

Final corrective validation used the compatible JDK 17/Android SDK 35 toolchain: `:core:test` and `:app:assembleFusionTest` completed with `BUILD SUCCESSFUL`. Packaged metadata was reread after that clean build and still reported the dedicated package, version, label and debuggable flag above. The final APK, checksum and revised checklist were then copied to both v2 delivery directories; the Drive APK was reread at 68,036,586 bytes and its SHA-256 matched `9744f913f7c40aaf7e4cbc1e2833a95cccb12e6eaf626f72be8df5a5ab8805d2`.

## 2026-10-09 — R3 phone validation: isolation passed

The user downloaded and installed the corrected v2 APK. Android installed it as a new app rather than offering the unsafe update path encountered with v1. On first open, **HS Deck Tracker Fusion Test** contained no historical decks or games from the working tracker. This confirms separate app-private storage on the device for the active `com.stroexd.hsdecktracker.fusiontest` identity.

At that point this was a passed new-app isolation check, not complete R3 device acceptance. Verification of the original tracker, active-match force-stop recovery, completed-match deduplication, evidence-import cancellation/retry and persisted fusion status were still pending. R4 had not started.

The user then reopened the original tracker and confirmed its decks and match history remained present and active. The phone-verified isolation gate therefore passes in both directions: the fusion-test app did not inherit working data, and installing/opening it did not remove or replace the original tracker's data. Active-match recovery and later R3 phone gates remain pending.

## 2026-10-09 — R3 phone validation: force-stop evidence

The user played turn 1, force-stopped during the opponent turn, relaunched Hearthstone and continued the game. The newly uploaded `HS-export-20261009-155235-2708d9c1.zip` reports partial success: 2 of 3 sessions exported and two session-local matches decoded. Direct manifest and report inspection shows these are not two independent coaching games. `Hearthstone_2026_10_09_15_42_59` is an incomplete 931-event fragment ending at 15:45:10 without `FINAL_GAMEOVER`; `Hearthstone_2026_10_09_15_45_23` is the 5,631-event continuation that completes at 15:50:46. The adjacent timing, shared participants and turn progression are consistent with the user-described stop/relaunch boundary.

The third selected session, `Hearthstone_2026_10_09_13_13_55`, is `READ_FAILED` because the exporter received a null `ParcelFileDescriptor`. It is retained as non-game failure evidence and is not treated as an empty or missing competitive match. The export warning is therefore expected and the saved ZIP is valid partial-success evidence.

At the user's direction, the logical match and both fragments were marked **FOR TESTING** and excluded from coaching. A machine-readable `HS-export-20261009-155235-2708d9c1.testing.json` marker was placed beside the ZIP, and the canonical Shared Exchange `START_HERE.md` now routes the same exclusion. The two readable fragments must be counted once for validation and zero times for coaching; the failed session must not be counted as a game.

This proves exporter-side continuity evidence, but it does not yet prove the fusion-test tracker's active journal UI recovered or deduplicated correctly. The user still needs to confirm that **HS Deck Tracker Fusion Test** shows exactly one completed match and no finished active draft reopens. Evidence-import cancellation/retry/status persistence also remains pending. R4 has not started.

The user's phone screenshots then confirmed one automatic completed record: LOSS, Rogue versus Shaman, six turns, duration 6:27, with coin. The timeline retains the pre-stop **Start** group and resumes visibly at Turn 3, followed by Turns 4 and 5 in the captured viewport; Turn 2 has no rendered row. The exporter spans 15:44:21.9269300 through 15:50:46.1418030 (about 6:24), so the tracker's 6:27 duration is consistent with preserving the original draft start across restart rather than starting a fresh post-relaunch record. The approximately three-second difference is compatible with tracker recognition beginning slightly before the first selected exporter match event.

`MatchHistory.turns` groups only persisted timeline events and emits no placeholder for an eventless turn. The missing Turn 2 row therefore proves a bounded recognition/timeline gap around the process interruption, not wholesale draft loss. It remains a useful defect signal because a normally recognized draw or play would have created a row. The screenshots do not yet prove that a finished inactive draft cannot reopen after a later process restart.

The phone's **Record diagnostics** option was off. Source inspection confirms it records OCR frames, recognized tracker events/notes and sampled screenshots only while enabled; it cannot reconstruct this past game. Diagnostics are optional for completing the current evidence-import gate, but should be enabled before any deliberate reproduction of the missing-turn issue and deleted after sharing because the ZIP includes screenshots and recognized on-screen text.

## 2026-10-09 — R3 phone validation accepted

The user opened the completed test match and exercised the evidence picker. Cancelling the picker produced no success claim, which is the intended cancellation behavior. Selecting `HS-export-20261009-155235-2708d9c1.zip` then produced **Fused artifact saved locally**. After reopening the fusion-test app, the saved fusion result remained available. This phone-verifies selection, multi-session bundle ingestion, unique conservative pairing, local artifact persistence and persisted visible status for the real force-stop/relaunch match.

Together with the previously confirmed separate installation/data boundary, preserved original tracker data, single completed 6:27 record and restart continuity, this completes R3 physical-device acceptance. The missing Turn 2 tracker row remains a documented capture gap around the intentional interruption; the successfully fused two-session evidence is the intended enrichment path rather than a reason for another optimization loop.

Cancellation and the successful path were exercised on-device. Permission denial, unreadable document handles, interrupted PROCESSING recovery, retry accounting and atomic replacement remain proven by deterministic automated tests rather than requiring the user to manufacture another phone failure. R4 has not started and requires an explicit phase decision.

The user clarified a prospective R4 device constraint: Hearthstone locks the phone to landscape while open, so arbitrary portrait/landscape gameplay rotation is not a useful acceptance exercise on this device. If R4 is later authorized, acceptance should instead cover landscape startup, lock/unlock, capture stop/reconnect and any fold/window-size or inset changes that remain possible in landscape. Coordinate handling must remain dimension-aware; the orientation lock narrows testing but is not permission to hard-code one resolution. No R4 implementation was started.

Further prospective R4 decisions: use Hearthstone-only single-app MediaProjection as the only pixel source; keep Accessibility, if used at all, to non-pixel foreground lifecycle detection; reuse existing OCR frames for trigger/manual-bookmark keyframes; and omit the rolling buffer entirely. Timestamp-only bookmarks remain available without capture. The user will monitor ordinary battery practicality and request a lower capture demand if necessary, so exact battery percentage is not a release gate. Obvious heat, game stutter or unbounded capture/storage remains a defect. These decisions narrow the future scope but do not authorize or start R4.

## 2026-10-09 — R4 authorized; usage-bound restart checkpoint

The user explicitly authorized the bounded R4 plan. R4 will add targeted visual-state enrichment on top of Hearthstone-only single-app MediaProjection, not a general full-screen recorder. It will start with normalized/configurable regions and bounded trigger/manual-bookmark keyframes, with no rolling buffer and with timestamp-only bookmarks remaining usable when visual capture is unavailable.

The initial pilot is limited to three coherent region groups: hero state; turn/resources; and event recovery through the card-play area, history rail and reconnect/resume cues. New observations begin in diagnostic/shadow mode. A field may progress from experimental to corroborating to accepted only after evidence-backed validation; ambiguous readings remain unknown or explicitly inferred, and an experimental recognizer cannot create an authoritative match event. Detailed board/minion recognition and other candidate regions are deferred rather than allowed to keep R4 open indefinitely.

Before implementation, the session reread the Shared Exchange protocol and canonical HSC backlog, confirmed R3 phone acceptance, verified repository `mrsox83/hs-deck-tracker-android`, branch `feature/mobile-fusion-offline`, dedicated package `com.stroexd.hsdecktracker.fusiontest`, clean local/remote tip `250f0b6`, and the preserved tracker-patch history recorded by the project checkpoints. HSC-006, HSC-007, HSC-009 and HSC-010 remain context and acceptance evidence, not blanket authority to implement their full backlogs in R4.

The live five-hour window was already 79% used (seven-day 36%) when authorization was received, above the owner's previously stated 60% ceiling. Therefore no R4 production code, test, APK, phone validation, merge or deployment was started. The exact next unit is R4.1 after a fresh usage reading. One reset credit was visible but was neither needed nor consumed.

The owner subsequently clarified that the tracker already has a screen-share/OCR module and R4 must extend it rather than reinvent capture. The earlier single-app-only constraint is superseded by an explicit user choice between privacy and capability: prefer Hearthstone-only single-app capture when supported, but permit an informed opt-in to the broader existing screen-share path with a warning that non-Hearthstone pixels such as notifications may be processed. Timestamp-only operation remains available. R4 still avoids a rolling buffer, unrestricted recording and unnecessary full-display persistence; the implementation task is now to audit/reuse existing frame sources and add bounded region/keyframe behavior plus clear mode disclosure.

## 2026-10-09 — R4.1 scheduled continuation started

The scheduled run began after the reset with live five-hour usage at 0% (seven-day 39%). The user-authorized ceiling for this window is 80%. The available full reset credit remains unused.

## 2026-10-09 — R4.1 source/build checkpoint

R4.1 reuses the existing `MediaProjectionSource`, `AccessibilityScreenshotSource` and recognition loop. Settings now offer three explicit policies: **Privacy first** requests Android's user-choice capture flow on Android 14+, **Compatibility** uses the broader existing screen-share/accessibility path with a non-Hearthstone-pixel warning, and **No visual capture** preserves the overlay and timestamp bookmarks without acquiring pixels. Android 13 and older cannot guarantee single-app capture and the disclosure says so. The stored evidence records the requested policy rather than claiming which source the Android consent dialog ultimately granted.

The existing MediaProjection lifecycle now responds to captured-content size changes and keeps frame coordinates dimension-aware. A serializable normalized-region primitive has deterministic edge/validation tests. Manual bookmarks always persist a timestamp; while capture is active, the next existing recognition frame is attached. Automatic evidence is restricted to named game-start, turn-change and game-end triggers. There is no rolling or pre-event buffer. JPEG keyframes are capped at 1,280 pixels wide, 200 files and 100 MiB, with SHA-256/dimensions/byte metadata; pending requests fail visibly when capture stops.

Validation passed the ordinary `:core:test` suite and `:app:assembleFusionTest`. Two first attempts encountered transient Windows locks inside generated Gradle output; deleting only the verified generated `core/build` or `app/build` directory and retrying resolved them. The final package task passed. Packaged metadata reports `com.stroexd.hsdecktracker.fusiontest`, version `1.5.0-fusiontest`, label **HS Deck Tracker Fusion Test**. The APK is 68,072,154 bytes with SHA-256 `27e68d7e54196c192838f99d14b1067e1ee11c6ca1f52078ad04d7094b124e19`.

At the clean source/build checkpoint, live five-hour usage was 43% and seven-day usage was 46%, below the 80% ceiling. The reset credit remains unused. R4.1 is source-ready/tests-passed/APK-built, but it is not phone-verified, merged or deployed. R4.2 has not started; the next required action is bounded physical-device validation of the three capture policies and lifecycle behavior.

## 2026-10-09 — R4.1 device-validation package v2

Preflight for the phone test found that retained R4 keyframes were internal-only, making the bounded-evidence acceptance claim unobservable without ADB. The existing **Share diagnostics** path now includes `visual-evidence.json` and retained R4 keyframes even when continuous **Record diagnostics** is off. **Delete** now clears both ordinary diagnostics and the repository-backed visual evidence/keyframes, providing the required user-controlled cleanup path without adding a second exporter.

The repository clear path has a durability assertion. The full ordinary core suite passed with 126 tests, and `:app:assembleFusionTest` passed after clearing only recurring locked generated Gradle directories. Packaged metadata still reports `com.stroexd.hsdecktracker.fusiontest`, version `1.5.0-fusiontest`, label **HS Deck Tracker Fusion Test**. The v2 APK is 68,073,322 bytes with SHA-256 `6984add76400fa8a2ea134a43bbd018cd3826a525029a6f077bbc57c4b39710c`.

The package, checksum and numbered privacy-first/timestamp-only/compatibility checklist are staged in both `C:\Users\Xs_da\Documents\SoxCoachExporter\Matches\mobile-fusion-r4-1-device-validation` and `H:\My Drive\HSReplay\Development\mobile-fusion-r4-1-device-validation`. The Drive APK was reread and matched the source hash and size. Five-hour usage was 54% after staging, below the 80% ceiling; the reset credit remains unused. Phone verification is now waiting on the owner; R4.2 remains unstarted.

## 2026-10-09 — R4.1 install blocked by debug-signing mismatch

Android accepted the unknown-source prompt but then reported **App not installed** for **HS Deck Tracker Fusion Test**. Direct comparison found the exact cause: both APKs use package `com.stroexd.hsdecktracker.fusiontest` and version code `10500`, but the installed R3 delivery is signed by certificate SHA-256 `eb353ba7cc17f2d8aa9247b166d327a4df9527a27b3d49af0ef8f4ec9a42bfb6`, while the new R4.1 delivery is signed by `4cf267d92ed361c32e2d4baa302670ee6c62a0cad042586e43de684ccc18f380`. Android therefore correctly rejects it as an incompatible update. The working tracker and existing Fusion Test data were not changed.

Do not retry, claim phone readiness, uninstall either tracker app, or begin R4.2 until the owner chooses a remedy. The safest available recovery is to create a backup from the existing Fusion Test app, preserve that file, uninstall **only HS Deck Tracker Fusion Test**, install the R4.1 APK, and restore the backup. Exact app-private continuity cannot be guaranteed by that route. Alternatively, locating the original private debug keystore requires explicit informed authorization because it is a sensitive signing credential; if unavailable, an in-place update is cryptographically impossible.

The owner explicitly authorized the sensitive keystore search after confirming this is the only current Android project. A filename-limited search of the user profile, including hidden paths, found only `C:\Users\Xs_da\.android\debug.keystore`, whose certificate is the newer R4 certificate. A separate search of the HSReplay Drive tree found no `.jks` or `.keystore` candidate. The original R3 signing key is therefore unavailable; no key was opened, copied, changed or uploaded.

Rather than delete or overwrite either installed app, R4 now uses a third isolated identity: `com.stroexd.hsdecktracker.fusiontestr4`, label **HS Deck Tracker Fusion R4 Test**, version `1.5.0-fusiontestr4`. This preserves the ordinary tracker and the accepted R3 Fusion Test installation/data side by side. `:app:assembleFusionTest` passed after one transient Gradle/Kotlin daemon crash was avoided with in-process compilation. Packaged metadata and signature were independently read. The 68,073,310-byte APK SHA-256 is `a884a626b0d5abe7b4033991b6bddfc2d744a8adc07dfd78f5e581f4a1310334`.

The corrected v2 package is staged locally and at `H:\My Drive\HSReplay\Development\mobile-fusion-r4-1-device-validation-v2`; the Drive copy matches the source hash and size. Android must offer **Install**, not **Update**. The earlier mismatched package is retained only as failed-attempt evidence and must not be retried. Five-hour usage was 68%, below the 80% ceiling. R4.1 phone verification is ready again; R4.2 remains unstarted.

Phone validation confirmed Privacy First does expose Android's single-app path, but the dropdown hid the selected policy and made Compatibility's intentionally disabled single-app option look like a failure. Replaced the dropdown with three always-visible radio choices, each with its full disclosure, plus a plain recommendation above: Privacy First for limiting pixels; Compatibility only when broader device support matters more. The rebuilt side-by-side R4 APK passed `:app:assembleFusionTest`; it is 68,073,870 bytes with SHA-256 `4748fd2843ffcee21053be09145feb70e41b3993f7ca6e42cc5c8302a443b745`. Usage reached 76%, so work stopped below the 80% ceiling. R4.2 remains unstarted.

The owner then confirmed that deck-tracking overlay persistence works, but supplied screenshot evidence of a narrow-layout regression: the new Bookmark icon expands the fixed header row enough to push Close beyond the visible right edge. This blocks R4.1 phone acceptance because the overlay must remain dismissible at every supported width. The next bounded fix is to pin Close in the always-visible action area and place Bookmark in a responsive secondary/overflow position; do not solve this by indiscriminately shrinking every control. Live five-hour usage reached the authorized 80% ceiling, so no source edit or rebuild was attempted. Resume after the 11:55 PM Central reset, fix and rebuild R4.1, then request the smallest possible phone retest. R4.2 remains unstarted.

## 2026-10-10 — R4.1 narrow-overlay source fix

The intended 11:55 PM automation did not launch: the anchored direct-create request was rejected and the fallback rendered only an unapproved suggestion, leaving no automation record. The owner noticed the missed run; live five-hour usage was 2%, so the already-authorized R4.1 unit resumed manually. This scheduling failure did not alter repository or phone state.

The layout cause was Material's minimum interactive touch target: four visually compact header `IconButton`s still required more width than the narrow curtain. Close is now the final pinned header action, Minimize stays directly visible, and a three-dot **More actions** menu contains **Bookmark this moment** and **Open app**. Touch targets were not indiscriminately reduced. `:app:assembleFusionTest` passed; package metadata remains `com.stroexd.hsdecktracker.fusiontestr4`, label **HS Deck Tracker Fusion R4 Test**, and the signing certificate is unchanged. The 68,078,034-byte APK SHA-256 is `95381a976019de83492f16e0dc28d2fd879e0d400d53be3b566cc806089bee53`.

The local and `H:\My Drive\HSReplay\Development\mobile-fusion-r4-1-device-validation-v4` deliveries contain the APK, checksum and a six-step layout-only retest; the Drive APK was reread and matched the source hash/size. No new game is required. Live usage was 16%, below the 80% ceiling. Source-ready/tests-passed-by-assembly/APK-built are true; phone-verified, merged and deployed remain false. R4.2 remains unstarted.
