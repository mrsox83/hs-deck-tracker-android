# Fresh reset-player restoration — bounded design review

Review only; owner approved shared reconciliation and next-issue review, not implementation/APK/publication. Existing global-turn /6 acceptance is separate. Related HSC-004/006/010 remain open. Existing pinned raw audit TURN-OWNERSHIP-RAW-AUDIT-2026-10-23.md supplies source lines; this review inspected current reducer/model, not a fresh private replay.

## What is missing

After gameplay rewind, some FULL_ENTITY Updating player headers contain a player name but no numeric instance ID. Following inline tags supply CONTROLLER/PLAYER_ID, but ENTITY_ID comes later. Current reducer safely clears the recipient rather than writing these tags onto the preceding card. It consequently drops supported restored player state. Inline CURRENT_PLAYER is not processed through the ordinary tag-based signal branch. This is not a Drive connector problem or proof of capture loss.

Pinned epoch4 example: header29123, controller29124/player ID29126, personal TURN29130, CURRENT_PLAYER29131; ENTITY_ID proof29140. Other player header29175, identity proof29190. Five resets/ten blocks in existing source. No checkpoint before29140 may be repaired using that later proof. No previous-epoch state or name-to-remaining-player guess.

## Smallest safe implementation boundary to approve next

1. Parse only supported FULL_ENTITY Updating player blocks in the reducer, with a bounded local accumulator. Require one positive unambiguous ENTITY_ID and consistent positive PLAYER_ID/CONTROLLER; reject game/card identity collisions, contradictory duplicate fields, missing proof and overflow.
2. Close the block on a new entity/header, non-inline boundary, reset or EOF. Do not commit immediately at the first ID if later tags in the same block could contradict it. Validate the complete bounded block, commit at completion, never backdate availability.
3. Restore only a reviewed player-state allowlist; do not interpret a player's TURN as global. No unrelated card restoration, observer inference, UI change, parser-wide rewrite or new collection/upload engine.
4. Fresh exact header alias may become usable only after validated binding. Placeholder names remain unknown. Do not import prior-epoch name mappings or final entities as authority.
5. Treat restored CURRENT_PLAYER as state establishment at commit, not a historical new-turn event. Earlier snapshots stay unchanged; do not replay actions, duplicate a turn, or claim player readiness after draw/start effects. Per-player indices remain unknown after reset unless separately proven; no parity or increment guess.

## Provenance contract must be resolved before coding

Current TagObservation/ValueObservation each carry a single EvidenceRef. Simply recording the original pre-binding line would make historical identity reconstruction see a value before it was attributable. Simply replacing it with the late ENTITY_ID line could misrepresent that line as the original tag observation and fail raw-value checks. Implementation must explicitly preserve BOTH observation origin and effective binding/availability cutoff, with truthful derivation labeling. Verify all consumers/validator/serializer behavior before selecting a representation. If existing evidence/rule fields cannot express it safely, propose a separately approved additive model change rather than silently backdating or inventing a witness. This is a review dependency, not permission for schema expansion.

## Required acceptance

- Synthetic valid late binding, both players, same-ID reuse only with fresh proof, exact and placeholder aliases; pre-proof/pre-completion snapshots remain unchanged, post-commit state correct.
- Reject conflicting/multiple/absent IDs/controllers, game/card collisions, block overflow, new-header/reset/EOF interruption without contamination. No cross-epoch carry.
- Inline CURRENT_PLAYER establishes state without duplicated signals/indices/actions; personal TURN never changes global /6 behavior.
- Evidence cutoff and raw-origin checks exercise serialization/validation and historical consumers, not only final entities.
- Pinned five-reset/ten-player-block private fixture, exact M.O.T.H.E.R. hand/cost regression and broader corpus. Preserve prior warnings unless exact evidence justifies their change; no blind golden updates.
- Independent source review; then separately gated source publication/APK/static/signature/alignment/delivery/phone validation. Existing artifacts stay immutable.

Scheduling: bounded planned P2 continuation of chronology work, not a new P0. Latest phone game had no reset, so it cannot accept this repair. Existing raw fixtures are sufficient to start; no new phone upload requested by default. No implementation or additional APK was made during review.
