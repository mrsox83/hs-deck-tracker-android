# HSC-006/010 consumer plan — 2026-10-19 restart

Status: prepared for owner review; implementation not started. Owner resumed the session. No APK, app integration, merge or deployment is implied by this plan.

October19 follow-up: this status applies to the hand consumer. Owner separately approved the growing-prefix prerequisite correction; it is now implemented locally with exact-content/raw-prefix equivalence and retained original provenance. See checkpoints/mobile-fusion-growing-prefix-review.md for verification and remaining logical-ID policy. No hand consumer, historical-artifact migration or APK is implied by that correction.

## Verified restart

Repository mrsox83/hs-deck-tracker-android, branch feature/mobile-fusion-offline. Clean local HEAD and independently read remote both equal 3117f992c249e630490a14b2bacf460c42f00072. Live usage1% five-hour/39%weekly at startup; ceiling90%, no reset consumed. Drive search and canonical Docs text read both succeeded today; prior connector failures are historical, not current evidence of unavailability.

Read canonical backlog revision AHj4eMSjegYLTME1_8Kzp795k7YqYdQ8B0yy5fabKLGiXyJuVSPHO3LOlD1_px-a1if6yMtYb3IT7fzy-gZedNW4kcdIT99uxL1nQBsZDQQ, Shared Exchange routing/receipts, and existing Example Rogue Deck A review-package overview, data contract, integration risks and acceptance criteria. Old priority rows use the historical P0/P1 meaning and conflict with the newer governance paragraph; they cannot automatically displace current coordinator triage.

## Proposed order after current intake

1. Bounded HSC-004/010 growing-log identity replay before hand implementation. Canonical October18 evidence reports the same completed match in a short prefix and a longer export, plus a genuinely new second match. Expected logical count: one DK WIN plus one Priest LOSS. Source adapters currently key source IDs on full Power.log hash plus match index; evaluator deduplicates by source ID; fused logical ID includes all selected source IDs. These are concrete risk paths, not proof the app actually duplicated history. Existing import-by-tracker-ID immutability may contain the symptom. Replay first; change production only if reproduction warrants it.
2. HSC-006/010 pure decision-hand projection using the contract below, after identity intake disposition. Keep the Example Rogue Deck A proposal REVISE/PLANNED, not implementation-accepted merely because its Python reference tests passed historically.
3. Draw and cross-midnight reports are regression references. DRAW already has directly corroborated outcome evidence; midnight note reports accepted pairing and latent timestamp qualification, not a reproduced wrong match. Neither self-promotes above identity integrity.
4. Owner-corrected Coin Rogue and Warrior remain two distinct real games; overlapping/stale timing remains uncertain. Do not discard the Rogue game or infer a merge from the superseded coaching interpretation. Partial opening coverage remains unknown.

This selects a read-only integrity investigation ahead of reconstruction, not an unverified P0/P1 assignment. HSC-004 remains P2 within its existing development triage; new reported cases remain UNASSESSED pending exact replay and comparison. LOCAL-001/002 and CLOUD-001 are DONE. Cosmetic labels/build locks remain lower-priority maintenance.

## First consumer implementation boundary (proposed)

Pure Kotlin projection over an immutable accepted FusedMatch; explicit checkpoint, controller, epoch, source and line cutoff. Reuse FusionSnapshotMaterializer and HistoricalIdentity. Do not port the Python reference unchanged.

- Replay snapshot order as stored. FULL replaces, DELTA merges; never sort stitched sources by their resetting source sequence or uncertain timestamps. Begin each epoch from its FULL state. No carry across reset or unsupported source gaps.
- Select only observed HAND entities with the requested time-scoped controller and positive integer ZONE_POSITION. Preserve unknown identity at known positions. Report position-zero entities, duplicates, missing positions and gaps explicitly. Do not infer compacted positions or rightmost arrivals in this first unit.
- Resolve card identity only through epoch/source/time-scoped identity history; never from final entity.cardId or printed CARD_ID assumptions. Snapshot entityTags alone are not a complete identity catalog.
- Use observed COST with exact evidence cutoff. Missing cost stays unknown. Validate tag-level source provenance from retained history; a checkpoint line alone does not prove every slot tag was observed there.
- Preserve checkpoint raw turn, active controller and player index as distinct nullable fields. An explicit controller selects an evidence view; it does not establish that controller is the human user.
- Keep outer BEFORE_ACTION and AFTER_OUTER_ACTION separate. Unresolved block boundaries cannot claim settled decision state. A source-reported target is not player intent.
- Hand completeness remains unknown unless coverage and inventory independently justify it; eight contiguous slots do not prove exhaustive capture. Unknown slot count is nullable when expected hand size is unknown.
- Epoch historical/current designation is a source segment label, not proof every pre-reset action was semantically undone. Do not score or sum replayed actions as extra activity.

Output initially an in-memory optional derivation with per-slot claims and diagnostics. No new FusedMatch handStates field, persistence, importer/exporter changes, UI, protected reads, uploader work or dependencies in the first unit. Mulligan reconstruction, inferred reorder fallback, cost simulation and coaching judgment are excluded.

## Concrete acceptance gates

- Exact Example Rogue Deck A #84 raw hash/accepted pairing, checkpoint sourceSequence14976 BEFORE_ACTION/sourceLine34937: eight observed slots 38,53,44,126,48,46,234,40; zero-position e67 excluded with partial/transitional warning. Expected identities/costs must be revalidated using regenerated /3 evidence; legacy prototype output is a reference, not an authoritative new-core PASS.
- Identity known later cannot leak backward, including transforms, same-line conflicts and reset entity-ID reuse.
- Duplicate positions, missing costs/controller, null expected count, DELTA before FULL, epoch change without FULL and source gap produce explicit unknown/error as appropriate.
- Cross-session boundaries retain stored source order and exact provenance; no source-line comparisons across different sources without a separately reviewed chronology join.
- Partial Coin Rogue opening remains unknown. Mulligan same-slot replacements remain a raw-evidence fixture outside this initial consumer scope.
- Run focused and ordinary core tests plus bounded private exact replay. Measure elapsed time/peak-memory implications before considering phone integration. No phone performance claim from JVM tests.

## Canonical reconciliation proposal — not yet applied

Append an HS-FUSION October19 bounded progress entry, preserving old reports: published code b9220b0/closeout3117f99, /3 producer versions, 229 ordinary tests plus four private checks across recorded runs, conservative unknown owners, reset/archive/choice provenance, pinned local-second witness. No APK/phone acceptance for /3, universal HSC-004 closure or HSC-006/010 completion. Acknowledge new growing-prefix, draw, partial-source, owner-correction and midnight notes as RECEIVED/READ, with priority unassessed until replay. Preserve the original queue/history; reconcile stale priority meanings separately under current governance.

The owner's standing preference requires the proposed canonical change to be broken down before approval. This file provides that reviewable proposal; no canonical Doc write has been made this session. Connector access is available, so absence of approval is the remaining write gate, not a claimed connector failure.
