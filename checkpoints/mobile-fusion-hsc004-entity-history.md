# HSC-004 entity and choice history — 2026-10-19

Owner approved correcting the evidence-preservation gaps identified after `b9c6a65`.

The reducer archives complete EntityState records before clearing a reset boundary and includes the final epoch. Fusion assembly retains these archives. The legacy entities view remains the final observed epoch. Session stitching merges archive records only within the same mapped epoch; a later reset prevents earlier entities from being restored into current state.

Choice stages carry a nullable chronologyEpoch derived from their exact evidence source lines. Missing locations or evidence spanning epochs produce unknown. Reset-bearing continuations retain separate choice records despite reused IDs; ordinary continuations retain their existing merge behavior. Summary output exposes each stage's epoch and warns against combining unknown or differing epochs.

Regression checks cover archived pre-reset tags, choice stages on opposite sides of a reset, and exclusion of historical-only entities from stitched current state. Legacy decoding remains supported with empty archives and unknown choice epochs. Historical snapshots alone never substitute for preserved identity histories.

This does not establish full HSC-004 real-corpus acceptance or authorize hand reconstruction. APK, phone validation, merge and deployment remain separate gates.

Final full ordinary core run passed in fresh isolated output: 226 tests; zero failures/errors/skips. Temporary routing script removed. Changes remain local for review; no commit or push in this unit.

## Exact raw acceptance — 2026-10-19

Mounted Drive rewind ZIP independently verified against raw Power.log SHA-256 `ddc8357c0cd8f87ed37e9d092d758402dd1467a41968ff257c61e1358866b71e`. Private replay asserts all six reset events, seven nonempty historical/final entity epochs, final entities equal the final archive, and choice-stage source lines agree with their assigned epoch. It exposed overly strict immediate-adjacency rewind classification: actual logs include one or two numeric tags on the same actor at the same timestamp. Classification now allows at most two such intervening tag events; stale or unrelated observations remain unclassified.

Existing raw reconnect fixture passes its accepted pairing/stitch, cross-source choice confirmation, exact provenance and fused validator checks. Final combined validation: all 226 ordinary tests plus these two private fixture tests pass with zero failures/errors/skips. Raw files and existing phone artifacts were read only. No APK or publication performed.
