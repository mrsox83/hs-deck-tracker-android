# Mobile fusion session-continuation checkpoint

Date: 2026-10-09. Scope: approved offline R0-R2 hardening only.

## Shared finding addressed

HSC-004/HSC-009 identified one logical 26-turn Rogue versus Death Knight match split by a Hearthstone reconnect/session rollover. The first Power source ends without FINAL_GAMEOVER after opening Deathwing choice 16. The later source restates the same Deathwing entity and four options, confirms Enthrall, continues play and reaches FINAL_GAMEOVER. HSC-010 requires both source provenance records to survive fusion.

The paired tracker record is `match_20261008T041335265Z_bb7a20f1d4ac2bb0787aceb75997fd0643cdece6d795dd3bfcfa661e75018739.json` (26 turns, LOSS, DEATHKNIGHT). The `20261007T224522751Z` tracker record is a different 13-turn WIN versus Priest and is deliberately not paired.

## Conservative acceptance

`PowerContinuationStitcher` accepts a two-source continuation only when all of these are present:

- the earlier segment is incomplete and not source-truncated;
- the later segment has an exact FINAL_GAMEOVER evidence line;
- the sessions are ordered with a gap of at most 180 seconds;
- the tracker endpoint is within 120 seconds of the completed segment and contributes `/timestamp` evidence;
- one open choice resumes uniquely with the same choice ID, type, source entity and offered entity identities, then gains confirmation;
- both player/controller identities overlap;
- at least three stable entity ID/card-ID identities overlap;
- raw turn is unchanged or advances by one.

Any failed requirement rejects the candidate instead of guessing. Source truncation is not treated as a reconnect. The known case passes all checks.

## Artifact behavior

The stitched reducer output has a `DERIVED_FUSION` source whose parent IDs are the two immutable Power sources. Events are concatenated in source order and renumbered without changing source sequence or evidence. Time-scoped entity histories are merged, choice 16 combines the earlier offer with the later confirmation, and the later full snapshot begins a new checkpoint after the gap. Both original source hashes remain catalogued.

The `hs-fused-match/1` artifact records the accepted continuation decision and exact source-line/tracker-pointer evidence. Provenance validation checks continuation evidence and derived-source parent existence. An explicit diagnostic retains the session gap; the stitch does not claim gap-free capture. Duplicate ZIP wrappers remain aliases of the same inner Power source and do not become independent witnesses.

## Validation

- ordinary `:core:test`: 114 tests, 0 failures, 0 errors, 0 skipped;
- private `:core:realFusionFixtureTest`: 6 tests, 0 failures, 0 errors, 0 skipped;
- isolated `:core:realFusionArtifactLifecycleTest`: 1 test, 0 failures, 0 errors, 0 skipped.

The private gate pins 32 tracker records and keeps the existing 21-bundle / 48-source-match exporter corpus. The isolated representative artifact is 11,033,664 bytes and decodes under the explicit 512 MiB heap gate. No private fixture was copied into Git.

## Boundary

This checkpoint closes discretionary R0-R2 hardening. It does not add the HSC-009 Android active-match journal, capture, permissions, persistence, UI, upload, merge or deployment work. R3 was authorized afterward with a separate test/debug identity; R4-R6 remain explicit later phase boundaries.
