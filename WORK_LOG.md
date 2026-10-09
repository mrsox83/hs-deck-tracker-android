# Mobile fusion work log

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
