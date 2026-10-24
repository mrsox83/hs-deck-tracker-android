# Global-turn phone export witness — October23

Bounded uploaded-export PASS, not universal phone/UI/HSC acceptance. Existing mounted originals read-only; ignored phone_turn_audit.py compares raw chronological GameEntity TURN at each exact cutoff, not a second JVM replay. No app changes/build/publication/old-artifact edits. ZIP HS-game-20261023-194555-dd47b4e9-logs.zip; saved tracker dd47b4e9-38d7-4cd8-aa8d-b762eecb372c, Rogue versus Shaman WIN, went second,8 tracker turns. Fusion android-r3/6, accepted source match1, completed/nontruncated; no GAME_RESET.

## Verified

- ZIP SHA256b97704c23d165b201137b355473b10e24dead7be180568f9ff8cb9deec765566 equals accepted artifact hash.
- Power SHA256efc1411c02f96f70fb124de2c050267266f12665049e1593a08b079fc0d509a8 and match-document f672ddc03448486dbaf3442cc3b070a90795e514c6b10a48a8b774e50576d8a5 match fusion provenance exactly.
- Fusion SHA256b036a2207cc6cc34d418a804210d4a25266eace7b1dad1145db8664821e8224d; standalone tracker05f417fc1dd66b31c8a70daffaf706603a2a20bb4a1811fc67fdddfa6d10620a. Known compact tracker serialization reproduces source hashdb6ca58e490a2b370ae53e36f8c62929e1fa0080fc6c58756010a274ef11be25; formatting relation, not byte-identical tracker files. Tracker ID agrees.
-302 canonical events,494 snapshots and494 hand checkpoints all agree with latest raw same-game global TURN at their source cutoff; no turn/epoch mismatch. Raw global turns1 through15 preserved. Example raw1832 global2 then1834 personal1;2612 global3 then2614 personal2;2920 personal2 does not overwrite global4. No future backfill inferred.
-41 tracker timeline observations exactly match standalone original in order.
-6624 repeated card/checkpoint observations,25283 hand evidence references checked for source/cutoff; positions ordered/unique and each has a matching raw position witness; supplied identity/recorded-cost refs and action-card entity links checked, no mismatch.

Initial audit incorrectly required every positionEvidence reference to be a ZONE_POSITION line, producing13248 false flags: reference sets also contain controller/zone prerequisites. Corrected audit requires a matching actual position witness within the already source/cutoff-validated set, while preserving all prerequisite checks. Final errors empty; no product or source evidence altered to obtain PASS.

## Limits

Seven unresolved-owner markers remain2611,4375,5890,7876,10773,15767,18371; unknown per-player indices remain disclosed. Descriptor disagreements327 position/340 zone explicitly retained, measured tracker coverage5 intervals not proof all actions recognized. Source-controller identity is not authenticated observer knowledge; no mana-paid/selected-intent inference. No reset occurred, so fresh reset-player restoration remains untested/unimplemented by this slice. This upload proves bounded export behavior, not every old-game/UI/performance gate. No broad HSC closure. No private account names/raw copied to Git.

Next: report bounded PASS to owner; approve Resource Exchange/canonical bounded acceptance reconciliation separately, then consider separately scoped reset-player restoration. No additional upload is needed to validate this demonstrated counter fix.
