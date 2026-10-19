# HSC-004 chronology field contract

This contract is conservative. It describes evidence; it does not reconstruct unsupported ownership or erase prior history.

- `sequence` is retained artifact order.
- `rawTurn` is the directly observed global Hearthstone `TURN` tag. It is not a per-player turn count and may repeat or move backward around a reset.
- `activeController` is present only when the current-player entity maps directly to a controller. It is never inferred from alternating turns.
- `playerTurnIndex` is present only while controller ownership has been continuously known. An unresolved owner or reset invalidates later indices unless a separately reviewed explicit re-anchor is added.
- `chronologyEpoch` is a zero-based, monotonically increasing source-order segment. Every observed `GAME_RESET` starts a new epoch.
- `chronologyState=CURRENT` identifies the final observed epoch. Earlier epochs are `HISTORICAL`: they remain evidence but must not be counted as additional current game activity.
- `GAME_RESET` is not automatically called a gameplay rewind. That classification requires directly observed `USED_REWIND=1`; otherwise the cause remains unclassified.
- Reset classification requires a fresh `USED_REWIND=1` observation for the reset actor at the same log time, at most three parsed events before the reset. Every intervening event must be a tag observation on that actor at the same log time. A sticky earlier tag is insufficient. This bound covers the exact raw six-rewind capture's intervening numeric tags.
- Entity state is cleared at the reset boundary. The first post-reset snapshot is a full baseline containing only state re-observed after the boundary; pre-reset state remains in historical snapshots.

For stitched reconnect sources, epochs remain monotonic across the logical match. A reset in the later source makes the earlier source historical. Without a later reset, the accepted continuation remains one current chronology even though its provenance spans sources.

This contract does not yet authorize hand reconstruction, hidden-information inference, merge, deployment, or rewriting saved artifacts.

Entity archives (`entityEpochs`) preserve complete observed identities, tags and histories separately for each epoch. The legacy `entities` view contains only final-epoch observations for newly reduced records. Missing archives in legacy artifacts mean historical epoch coverage is unknown.

Each choice stage has its own nullable epoch, derived from exact source lines. Missing locations or observations spanning boundaries produce UNKNOWN. An offer and confirmation in different epochs do not prove a completed current choice. Reset-bearing source continuations preserve separate choices even when IDs repeat; no cross-reset ID merge is permitted.

New output versions are `offline-r2/3` and `android-r3/3`; the additive schema stays `hs-fused-match/1`. Legacy artifacts are preserved. Empty epoch archives trigger an explicit unverified-history warning. HistoricalIdentity resolves card identity only within the requested epoch, source and source-line cutoff, with no fallback to later or other-source final claims.
