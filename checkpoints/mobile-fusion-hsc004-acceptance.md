# HSC-004 acceptance checks — 2026-10-19

Owner approved archive validation, output version distinction, first/second raw starting-owner checks and prevention of future identity leakage.

- New producer versions: offline-r2/3 and android-r3/3, additive hs-fused-match/1 schema retained. Legacy archive absence warns explicitly; existing artifacts are not regenerated.
- Archived entity claims and all retained observation histories receive the same exact-source/location validation as current entities. Archive epoch IDs must be distinct, nonnegative and increasing; current entities must equal the final archive.
- HistoricalIdentity.cardIdAt requires an exact epoch, source ID and source-line cutoff. Missing/other-epoch/other-source history yields unknown; later reveals cannot resolve earlier identity. This does not infer hand contents or player perspective.
- Raw fixture HS-export-20261007-110747-2272cd33.zip is hash-pinned by the private fixture inventory. Raw SHA-256 a9b98032384fbaeaa1430d3c39a27f68be2db0716de739465e6ebe623bfde53b has game1 controller1 starting (current-player source line343) and game2 controller2 starting (line31944). Both agree with explicit FIRST_PLAYER tags and index1. This covers both controller starting positions; it is not exhaustive local-user ownership or later-turn recovery acceptance.
- Exact six-rewind and reconnect fixtures remain passing.

Initial automated gate: 228 ordinary tests and three private raw replay tests, zero failures/errors/skips. Independent reviewer identified archive alignment/coverage, /3 archive enforcement, recursive error attribution and same-line identity ambiguity. Corrections enforce contiguous archives aligned with event/snapshot epochs, require archives for accepted /3 event chronology, attribute only entity errors to archive paths, and return unknown for conflicting same-line identities. Added malformed archive tests; final gate follows these corrections.

No APK or existing-artifact rewrite. Full local-player perspective and later-turn reconstruction remain unproven. Hand/mana/decision reconstruction remains gated; the starting-controller fixture alone does not satisfy that broader criterion.

Final gate after review corrections: 229 ordinary tests plus three private replays pass, zero failures/errors/skips. Existing Rafaam tracker explicitly reports wentFirst=true; the reconnect tracker reports wentFirst=null. Neither substitutes for a verified local-player went-second fixture. That acceptance criterion remains open. Temporary routing script removed. Changes remain local, uncommitted and unpublished.

## Local went-second evidence — follow-up

Discovery across existing uploads found a unique uninterrupted pair with sufficient turn coverage. The repeatable test now pins only:

- Tracker `match_20261008T210742979Z_2d002b13fdc7b8819a62d26100dc81bf7e53e3b83d9d9066915564dca160dcc0.json`, SHA-256 `57c9e135926c2811cae9cbd83bc5a18f71b1bc13eb9c8ca4979f5c1c7b4a771f`, explicitly wentFirst=false.
- Bundle `HS-export-20261008-161122-04f747f4.zip`, SHA-256 `3034236b5d61e1f75d0ace581710952cbc1c969ed9c9fec25ec4a2f6b649e92e`.
- Inner source `power:b3f2f5c912db77f821b47cfa2611d32ba51366c686813eb4646c831c06ea0ed4:1`, local controller1, 30 TURN_SIGNAL snapshots.
- Explicit other-player FIRST_PLAYER=1 at line466; opening owner signals at lines465 and2192 are controller2 then controller1. Local PLAYSTATE result at line54759 corroborates tracker result. Endpoint metadata is corroborated by retained FINAL_GAMEOVER evidence.

This derives went-second from two directly observed opening owners and an explicit first-player identity, without assuming an absent FIRST_PLAYER flag equals zero. Independent read-only review accepted this reasoning and required exact fixture pinning, now present. Each resolved owner is additionally checked against raw CURRENT_PLAYER=1 and a direct ID or alias observed by that source line; per-player counts follow observed ownership transitions and remain unknown after unresolved ownership.

This is one bounded uninterrupted local-went-second witness. It does not establish universal ownership recovery, unknown intervals, resets or reconnect perspective; those retain their separate acceptance boundaries. Final pinned replay result follows below.

Final pinned replay: PASS, one test, zero failures/errors/skips. Combined evidence is the previously completed 229 ordinary tests plus three private acceptance checks and this additional private went-second check. No production code changed during this follow-up. The local-went-second witness criterion above supersedes the earlier open wording; full HSC-006/010 hand reconstruction remains separately gated. Temporary build-routing script removed; acceptance changes remain local and unpublished.
