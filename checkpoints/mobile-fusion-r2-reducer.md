# Checkpoint: R2 reducer foundations

Date: 2026-10-08. Offline R0–R2 scope only.

Implemented and tested:

- ordered nested action stack with canonical parent links;
- no-target semantics for Power target `0`;
- time-scoped CardID identity revisions with exact evidence pointers;
- raw tag history with unknown tags retained;
- typed observed counters for resources, overload, health/damage/armor, Herald, quest progress and turns-in-play;
- separate offered, submitted and confirmed choice stages;
- one pre-action snapshot at outer BLOCK_START;
- one post-resolution snapshot only after the matching outer BLOCK_END;
- unresolved last-valid snapshot when blocks do not close;
- raw turn and active-controller fields retained when evidence resolves them.
- typed zone, position, controller and visibility histories;
- observed CREATOR/copy/hero/entity links without card-name heuristics;
- raw-log-backed player-name aliases with source-line provenance;
- per-controller turn indices advanced by resolved CURRENT_PLAYER transitions, not parity.
- typed player snapshots joined through observed HERO_ENTITY relationships;
- current-hero remaining health derived only from observed HEALTH and DAMAGE, with both evidence references;
- armor, permanent resources, used resources, temporary resources and overload retained as separate claims.
- quest progress, total, explicit completion and reward entity retained as separate claims;
- Herald amount and class observed separately from unknown build-specific threshold interpretation.
- endpoint, result, first-player and card-anchor pairing agreements accepted only with exact Power source lines plus tracker JSON pointers;
- fused-artifact validation requiring every non-unknown typed claim and accepted pairing to reference a known source at an exact line or JSON pointer;
- provenance validation for canonical events, entity tag/identity/zone/position/controller/visibility/alias histories, choice stages and snapshots.

Real fixture assertions confirm that the Rafaam match contains time-scoped identity revisions, zone and visibility transitions, hero links, per-controller turn indices and provenance-bearing RESOURCES_USED and HERALD_COLOSSAL_AMOUNT snapshots. Pairing facts are derived from the reduced log for local controller 2, and the complete fused artifact passes provenance validation. Synthetic assertions cover nested blocks, exact counter source lines, aliases, typed transitions, unknown tags, incomplete blocks, evidence-free pairing rejection and invalid fused evidence.

Not yet accepted:

- permanent/temporary/used/overload mana semantics beyond observed values;
- game-build-specific Herald threshold interpretation;
- readiness, attack availability, deck membership/origin and generated/transform links;
- extra-turn semantics beyond observed controller transition counts;
- additional game-build semantics or fixture-specific facts beyond the approved R0-R2 evidence model.

No Android capture, UI, permission, persistence, merge, push or deployment work is included.
