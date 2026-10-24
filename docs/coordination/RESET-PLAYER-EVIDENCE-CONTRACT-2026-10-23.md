# Reset-player evidence contract — locally implemented, acceptance gates separate

## Implementation clarification — owner-approved bounded task

Raw/source and prefix-test review exposed two errors in the initial wording below; this paragraph supersedes those details without changing the no-backdating constraint. Block closure is first knowable when the next non-inline record is read. Use THAT boundary line as the effective commit cutoff, before processing its gameplay, and retain a separately labeled boundary witness. The original tag remains a distinct origin witness; the boundary is not falsely described as a direct tag observation. Using the last inline line would authorize a prefix that has not yet established closure. Likewise valid resource/Herald tags occur after ENTITY_ID in the real source: require header<origin, header<identity, max(origin,identity)<commit rather than origin<=identity. All witnesses must share the source/epoch. Neither ordering changes prior snapshots or trusts a future identity.

SupportingEvidenceRef is a separate nonrecursive leaf type, not recursive EvidenceRef with a runtime nesting restriction; this enforces the intended flat contract structurally. Default empty proof is omitted even with encodeDefaults enabled, retaining old ordinary-ref encoding. Proof roles are header/origin/identity/player/controller/type/boundary. Primary derivation rule identifies entity and field. Claims require exactly DERIVED and validate proof regardless of status. Reversed card/GameEntity collisions quarantine current restored state while preserving already captured snapshots/history; restored aliases cannot establish continuation overlap.

Owner initially approved narrow design, then approved bounded implementation/tests with an exception to the85% ceiling for this last task. Base966354c; /6 delivered APK remains unchanged. /7 source implementation is local/unpublished, no APK or raw changes. Final acceptance evidence belongs in the reset-player checkpoint; this contract does not claim delivery or broad issue closure.

## Selected direction

Use an explicitly DERIVED restoration assertion at the validated block's commit cutoff. Preserve earlier tag/header/ENTITY_ID origins as structured supporting evidence. Do not retain original tag availability in the primary historical line; do not pretend the commit line literally contains the restored tag. No earlier checkpoint changes. Current raw source documents remain authoritative originals.

Additive EvidenceRef field: `supportingEvidence: List<SupportingEvidenceRef> = emptyList()`. Leaf type carries sourceId/sourceLine/role and cannot recurse. Require exact positive lines and same accepted Power source. Default empty proof is explicitly omitted with EncodeDefault.NEVER even when AppJson encodes other defaults; ordinary-ref compatibility is tested.

For a delayed restored tag, primary EvidenceRef.sourceLine and TagObservation.sourceLine/sequence/logTime identify COMMIT availability, with a fixed recognizable restoration derivation rule. Its supporting refs identify original tag, player header and late ENTITY_ID proof; also retain block closure witness if different from commit boundary. A named role/rule on each leaf distinguishes these witnesses. The primary line is a derivation cutoff, explicitly not a direct raw tag observation. Derived player claims must use DERIVED status/reason, not OBSERVED-at-commit wording. Restore only reviewed player fields, not cards or costs, in this first slice.

Example from existing raw audit: player header29123, CONTROLLER tag29124, ENTITY_ID29140, later block completionC. Controller is usable atC and later only; its proof preserves29124 and29140/header. It is unavailable at29131 and at29140 if the same block is not complete. Raw global TURN handling is untouched.

## Why this direction

- Backdating primary refs to original tag lines leaks later attribution into earlier history; rejected.
- Changing only sourceLine to the proof line loses original tag proof and misleads raw checks; rejected.
- Keeping original line and adding only availableFrom requires every old consumer to understand the new cutoff, otherwise readers that ignore optional fields leak information; rejected for this bounded addition.
- Effective primary refs plus explicit supporting origins preserve conservative cutoff behavior in current readers. Older readers may ignore new supporting origins, so do NOT claim they validate the full new derivation; phone/runtime and downstream compatibility need separate acceptance.

## Inspected consumers and required changes/tests

| Consumer | Current behavior | Required acceptance |
|---|---|---|
| HistoricalIdentity | filters identity refs by primary sourceLine | No restored card identity in first slice; old observations unchanged |
| DecisionHandProjector | requires tag sourceLine==evidence.sourceLine and cutoff | Effective primary lines remain aligned; no pre-commit player ownership |
| CoachingHandHistoryBuilder | same-source/cutoff tag and identity checks | No restored card/COST path; existing exact hand/cost witness unchanged |
| PowerPairingFacts | historical PLAY anchors scoped by source/epoch/cutoff; final player facts for result/first | No earlier anchor authorization; restore no terminal/first-player facts in first slice; verify candidate status drift independently |
| PowerContinuationStitcher | merges histories distinctBy evidence; alias/controller joins | Supporting proof participates in equality; distinct original observations may not collapse at shared commit; no new alias-to-continuation authority without source/cutoff proof |
| Snapshot counters/player claims | currently use one observation ref and OBSERVED reasons | Restored fields must propagate derived proof/status, commit only after full-block validation |
| FusionProvenanceValidator/codec | checks source/location, not delayed binding shape | Validate derivation structure, flatness, source/epoch/ranges/order/allowlist; reject malformed imported bundles, round-trip exact proof |
| Private raw audit tools/coaching consumers | some assume primary line literally has tag value | For recognized restoration rule follow original-tag supporting ref; ordinary rules keep direct raw checks. Document this semantic distinction |

Strict restoration rule/containing observation ties proof to entity/field. Validate header<origin, header<identity and max(origin,identity)<commit; all source/epoch bounds checked, including archived claims against root reset boundaries. A boundary witness equals commit. Validator has no raw bytes and cannot authenticate raw semantics merely from serialized lines; producer/private raw tests remain required. Reject absent or unknown derivation context.

## Bounded restoration and compatibility policy

Complete-block buffering only, strict finite line/tag/text bounds, discard on reset/invalid interruption/conflict/overflow. Exact positive ENTITY_ID and consistent PLAYER_ID/CONTROLLER; no ID collision with game/card entity. No intermediate snapshot mutation. First subsequent non-inline record provides boundary recognition and effective commit location BEFORE that record's gameplay is processed; it is not described as a direct tag observation or completed gameplay witness. Reject EOF blocks by default. Do not promote early ENTITY_ID as final if later same-block contradiction is possible. Later direct same-role controller evidence can supersede derived state under existing direct-tag semantics; cross-role collisions quarantine current restored state.

Supported first-slice state: PLAYER_ID, CONTROLLER, ENTITY_ID, personal TURN, CURRENT_PLAYER, HERO_ENTITY and explicitly reviewed resource fields only. No FIRST_PLAYER/PLAYSTATE/card identity/COST inference; exact header alias after commit only, nonplaceholder and collision-free. Duplicate/conflicting current-player state leaves active owner unknown. Restored CURRENT_PLAYER sets state without fabricating past TURN_SIGNAL or player-turn index; normal later raw signals retain existing semantics. Per-player indices remain unknown after reset unless separately proven.

Keep schema hs-fused-match/1 only if additive default decoding and safe omission/reader behavior are demonstrated; advance producer from /6 for newly emitted restoration semantics. This is a proposal, not an existing format guarantee. No rewriting/migrating historical artifacts, no new dependency/permission/UI/collection engine.

## Implementation test gate

1. Codec old-ref compatibility, new flat proof round trip; reject nested/cross-source/future/wrong-epoch/missing/ambiguous proof.
2. Prefix replay at before tag, identity proof, before closure, after closure: state unavailable until commit, older snapshots unchanged.
3. Valid both-player/same-ID fresh restoration; conflict/overflow/new-header/reset/EOF failures; stale pending recipient and alias collision containment.
4. Same-commit distinct tag origins survive equality/stitching; derived status and source refs survive claims/snapshots/exports.
5. CURRENT_PLAYER no duplicated action/signal/indices; player TURN never becomes global; no change to /6 unknown alias policy outside proven fresh headers.
6. Existing five-reset/ten-block private witness, exact M.O.T.H.E.R. package, current /6 no-reset game and broader corpus. No blind warning/golden flips. Independent review before publication/APK.

Next owner gate: approve this additive evidence model plus bounded reducer/validator/consumer implementation and tests. Delivery/phone acceptance remain separate. Current69%five-hour intake under85%; no large implementation/test loops started in this design unit.
