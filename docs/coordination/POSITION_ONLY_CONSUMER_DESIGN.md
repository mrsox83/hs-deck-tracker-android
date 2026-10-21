# Position-only hand consumer — bounded design

Status: DESIGN COMPLETE / IMPLEMENTATION PROPOSED. Owner approved design only. Baseline d695dcd, feature/mobile-fusion-offline. No production edits/tests/build/publication. Existing264 ordinary/12 private test results are historical, not rerun. Intake7%five-hour/22%weekly under80% ceiling; no reset credit used.

## Outcome and boundaries

Owner clarification: PLAYER HAND ORDER is the required outcome. Opponent hand order is optional/best-effort and must not block implementation, acceptance or delivery of the player view. Do not expand opponent capture/reconstruction to satisfy this unit. Existing both-controller projector support/regression tests remain intact, but are not a requirement to display both hands.

Player selection must be explicit: require one positive selected controller, with its selection basis labeled (for example caller/user selection, not independently verified local observer). Do not default player=1, derive from turn parity or promote ObserverAssociation CANDIDATE to verified player identity. A single-controller report can remain useful with this honest selection label; lack of authenticated observer proof does not block numeric hand-order output. Later phone controls must explain the selection without presenting an annotation as verified knowledge.

Expose the existing DecisionHandProjector as a derived, read-only checkpoint report. No saved FusedMatch mutation/schema migration, identity enrichment, observer mapping, cost inference, coaching judgment or automatic uploader. A checkpoint is a source checkpoint, not a certified pre-decision knowledge boundary. Preserve the existing compact summary contract; its pilot excludes hand reconstruction and should not silently change meaning.

## First proposed implementation unit

Add a core-only position report renderer with an entrypoint taking FusedMatch, one explicit checkpoint ID, one explicitly selected player controller (no default), cancellation callback and an explicit output-size bound. Project only that controller in the required path, using DecisionHandProjector once; no independent hand reconstruction. Optional opponent output is off by default and may be deferred entirely in the first implementation. If later added, it must not let opponent-only ambiguity or unavailable slots suppress an otherwise valid player report; shared artifact/provenance failures still fail safely. Validate supported schema and accepted pairing via existing projector. Do not fall back to a nearby checkpoint, final entity state, another epoch or a guessed source. Missing/ambiguous checkpoint or invalid provenance remains a clear failure, not an empty hand. No app wiring/SAF/storage change in this first unit.

Render these fields:

- Derived-view/version marker; match ID/revision/fusion version; selected checkpoint ID/phase/source epoch and source location where supplied by slot evidence. Raw turn/active controller/player-turn indices stay numeric/UNKNOWN; never use parity or relabel controllers as human/opponent.
- Banner: observed hand positions only; identities and private costs withheld; not proof of complete hand, human ignorance or pre-decision knowledge.
- Per requested controller, existing ordered slot position and source-scoped entity reference, card identity UNKNOWN and effective cost UNKNOWN. Withhold identity/cost unconditionally in this version even if a caller constructs a populated DecisionHandSlot; renderer must not accidentally become a knowledge interface.
- Per-slot evidence source/line/pointer/rule; all projector diagnostics, including descriptor supplementation, duplicate positions, gaps and unknown completeness. Show phase, unresolved block and chronology state without reclassifying them as verified decision boundaries.
- Empty observed slots means no evidenced slots in this view, not empty hand. expectedHandSize/complete remain UNKNOWN unless future independently supported policy explicitly changes the contract. Do not fill missing positions or invent unknown cards to reach a guessed hand size.

All artifact-supplied strings are untrusted data: use existing summary's escaping policy (controls/Unicode format/punctuation/entities), including IDs, diagnostics and evidence. Reuse a small shared formatter only if tests preserve existing compact summary byte behavior; otherwise keep a bounded local formatter to avoid widening this unit. No HTML/raw Markdown injection, raw OCR, card catalog lookup, alias strings or private account values. Numeric controller is not verified observer.

## Bounds and failures

One checkpoint per call. Explicit deterministic maximum output size; fail visibly on overflow rather than emit silently truncated evidence or omit completeness warnings. Exact default/limit chosen with focused tests before implementation acceptance. Propagate cancellation; do not swallow it into UNKNOWN or partial success. No loop projecting every snapshot: existing projector materializes the prefix, so bulk repetition could be expensive. This renderer adds no whole-game throughput claim. Include long/malicious inputs and a large synthetic checkpoint smoke check before later Android integration; no phone latency/battery claim from JVM tests.

## Required tests

Required player selection tested for both controller1 and2 with no default/inference. Selected-player order and duplicate entity/card-instance labels remain source-scoped; gaps/duplicates/supplemental evidence explicitly retained; empty observed view not empty-hand claim; missing archive/legacy source limitations stay visible; invalid pairing/schema/checkpoint/provenance fail without fallback. Opponent output absent by default; missing/ambiguous opponent slots do not block a valid selected-player report. Optional opponent rendering tests apply only if implemented; no new opponent acquisition acceptance gate. Rewind/source transition tests preserve projector policy. Names/costs remain UNKNOWN even when constructed view fields contain values. Escaping covers every text field; cancellation/overflow deterministic. Rendering leaves source model and encoded saved artifact unchanged. Existing both-controller projector/private raw order tests retained, not weakened. Run focused tests, ordinary core suite and relevant private regression locally before review; independent review before publication/app integration.

## Later delivery choice — separately approved

After core validation, choose an explicit match-detail action such as 'Export observed hand positions' with checkpoint selection displaying source epoch, raw turn, phase and source line. Do not auto-select by turn alone. Background/cancellable generation, Android save picker and a separate clearly named text/Markdown file; exact format/schema and filename sanitization to be designed at that gate. Include match revision/checkpoint identity and preserve existing accepted fusion exports unchanged. Local save is not Drive upload. Do not bundle into every existing ZIP or alter download buttons by surprise. Multiple-checkpoint/bulk export requires separate size/performance design. No APK until the app implementation/review/static/signature/device gates and owner delivery approval.

## Next owner gate

Approve only the core-only single-checkpoint renderer and tests described above. No further observer audit or phone capture is a dependency for this position-only unit. Automatic decision-time knowledge remains DEFERRED; this report must not be advertised as full known-card coaching or current installed behavior. Resource Exchange patch notes are updated when an actual source-only implementation handoff exists, not as a fictitious shipped fix. No canonical issue closure/priority change, commit/push, merge or deployment authorized by design approval.
