# R5 current-session-first discovery checkpoint

Date: 2026-10-17 Central

## Owner-approved behavior

- The first **Find matching logs** action inspects only the highest-ranked current/most-likely session for the selected saved game.
- If that session does not produce an accepted candidate, the existing explicit **Search next 3** action reads up to the next three ranked sessions.
- Previously saved evidence and reduced results are reused; an expansion never rereads the first session.
- Total automatic scope is now four sessions rather than six. Existing 64 MiB raw, 32 MiB decoded, 24-match, per-session, cancellation, provenance, ambiguity and 60-second per-action limits remain unchanged.
- Manual one/two-session recovery remains available. Unreadable or unsearched sessions are still not ruled out, and confirmation still requires the existing supported unique-candidate gate.

## Validation and boundary

- Focused `PostMatchDiscoveryTest`: PASS, 13 tests.
- Regressions cover one-session initial scope, explicit next-three expansion, no duplicate reads, cumulative four-session cap, timeout retention, unreadable-session handling, ambiguity and saved-candidate review.
- Source-only for the next otherwise-needed build. No APK assembled or uploaded; no phone acceptance, merge, deployment, background collection or R6.
