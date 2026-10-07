# Mobile coach journal

The mobile tracker records conservative evidence for coaching without adding manual controls to the in-game overlay.

## Phase 1: friendly card plays

Completed match JSON now includes `PLAYER_PLAY` timeline events. The event uses the same compact timeline shape as the existing draw and opponent-play events:

```json
{"t":3,"k":"PLAYER_PLAY","d":12345}
```

- `t` is the friendly turn number used by the tracker.
- `k` identifies a probable friendly card play.
- `d` is the card's dbfId.

The vision tracker emits this event only after the complete recognized hand-row reading remains stable across at least four frames and two seconds. It can reconcile several cards played between two hand inspections without becoming permanently blocked by the first missed play. This remains conservative visual evidence; it does not infer attacks, targets, or exact play order within the same observation window.

`PLAYER_PLAY` is probable visual evidence, not a deterministic game-log event. Choice and replay effects may delay what the screen reveals, and reconnects can remove corroborating history. Consumers should retain the observation while avoiding unsupported claims about targets or precise timing.

The event survives deck recognition or a manual deck selection during the active match and is included in the normal completed `MatchRecord` JSON export.

## Next phases

Phase 2 will persist an in-progress match journal without counting it as a completed result. Phase 3 may use the Hearthstone history rail as optional confirmation. Neither behavior is part of Phase 1.
