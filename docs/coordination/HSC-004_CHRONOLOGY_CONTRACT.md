# HSC-004 chronology field contract

This contract is conservative. It describes evidence; it does not reconstruct unsupported ownership or erase prior history.

- `sequence` is retained artifact order.
- `rawTurn` is the directly observed global Hearthstone `TURN` tag. It is not a per-player turn count and may repeat or move backward around a reset.
- `activeController` is present only when the current-player entity maps directly to a controller. It is never inferred from alternating turns.
- `playerTurnIndex` is present only while controller ownership has been continuously known. An unresolved owner or reset invalidates later indices unless a separately reviewed explicit re-anchor is added.
- `chronologyEpoch` is a zero-based, monotonically increasing source-order segment. Every observed `GAME_RESET` starts a new epoch.
- `chronologyState=CURRENT` identifies the final observed epoch. Earlier epochs are `HISTORICAL`: they remain evidence but must not be counted as additional current game activity.
- `GAME_RESET` is not automatically called a gameplay rewind. That classification requires directly observed `USED_REWIND=1`; otherwise the cause remains unclassified.
- Reset classification requires a fresh adjacent `USED_REWIND=1` observation for the reset actor; a sticky earlier tag is insufficient.
- Entity state is cleared at the reset boundary. The first post-reset snapshot is a full baseline containing only state re-observed after the boundary; pre-reset state remains in historical snapshots.

For stitched reconnect sources, epochs remain monotonic across the logical match. A reset in the later source makes the earlier source historical. Without a later reset, the accepted continuation remains one current chronology even though its provenance spans sources.

This contract does not yet authorize hand reconstruction, hidden-information inference, merge, deployment, or rewriting saved artifacts.

The epoch classification currently covers canonical events and snapshots. Choices and the artifact's final aggregate entity history are retained evidence but are not yet epoch-classified; consumers must not treat them as current-state reconstruction across a reset.
