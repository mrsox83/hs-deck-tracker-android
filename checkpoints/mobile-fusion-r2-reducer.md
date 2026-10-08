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

Real fixture assertions confirm that the Rafaam match contains time-scoped identity revisions and provenance-bearing RESOURCES_USED and HERALD_COLOSSAL_AMOUNT snapshots. Synthetic assertions cover nested blocks, exact counter source lines, unknown tags and incomplete blocks.

Not yet accepted:

- controller/name resolution for every historical log shape;
- hero replacement relationships and safe remaining-health derivation;
- zone/position histories as typed structures rather than raw tag histories;
- permanent/temporary/used/overload mana semantics beyond observed values;
- quest completion/reward and Herald threshold interpretation;
- readiness, attack availability, deck membership/origin and generated/transform links;
- explicit player-turn indices and extra-turn semantics;
- broader real-fixture critical-fact assertions.

No Android capture, UI, permission, persistence, merge, push or deployment work is included.
