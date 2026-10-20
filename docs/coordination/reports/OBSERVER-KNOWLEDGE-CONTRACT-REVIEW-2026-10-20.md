# Observer-knowledge contract review — 2026-10-20 Central

Status: REVIEWED / NEXT AUDIT PROPOSED. Source baseline c28ad9d19ff5c8a90e4b41fb8aa0809fa49b6fd6, feature/mobile-fusion-offline; implementation source e2dfa78946720f2e80d3ff07e9f21c50d65af791. Source inspection only: no new private replay, tests, implementation, schema change, APK, publication or canonical write. Untracked growing-prefix helper preserved. Live usage6% five-hour/59%weekly at review start, ceiling90%.

## What exists, and what it does not prove

- DecisionHandView.kt preserves evidenced ordered positions for both requested controllers, labels supplemental descriptor support and deliberately withholds all identities/private costs. This is a safe position-only foundation, not the known-card contract.
- FusionEvaluation.kt:38–57 tries controller candidates for pairing. PairingDecision/FusedMatch in FusionModel.kt:222–250 retain the accepted source but no verified observer-controller mapping. Accepted pairing is not proof of which controller is the human or whose private information was visible. Outcome/first-player agreement, user edits, class/card matches and controller numbers cannot alone certify observer knowledge.
- HistoricalIdentity.kt:5–12 resolves source/epoch/cutoff identity observations. This answers what the source contained, not who knew it. It must not independently populate decision-time coaching names.
- FusionModel.kt:66–89 records identity history and boolean visibility history without an observer audience or knowledge scope. PowerEvidenceReducer.kt:225–230 records SHOW/HIDE booleans; those flags alone cannot certify availability to the selected human observer. Their raw semantics require evidence review, not an assumption that SHOW means public or HIDE erases human memory.
- PowerEvidenceReducer.kt:453–455 suppresses repeated unchanged identity values. A later reveal of an already source-known ID therefore is not necessarily represented as a fresh identity observation. Future knowledge logic cannot require or infer a new identity-history entry for every reveal.
- Stats.kt:41–46 stores compact turn/type/card timeline events without entity-instance/slot/source-line alignment. Recognition coverage is observation eligibility, not evidence of which exact hand card the observer saw. The game-wide opponentCards list remains a separate reference, not a time-local hand join.

## Decision boundary needs its own proof

PowerEvidenceReducer.kt:176–178 observes actor/target descriptor identities before capturing the outer BEFORE_ACTION snapshot at the same raw line. HistoricalIdentity currently permits observations at or before that line. Consequently a future naive identity join could use the action's own reveal to judge the preceding decision. Current production projection does not leak this because names/costs remain null.

The next contract must distinguish a source checkpoint from a certified pre-decision knowledge boundary. Simply renaming the phase, or changing <= to < everywhere, is not an established fix: a reveal may be logged in an earlier part of the same action. Audit the raw ordering before specifying that rule. Preserve stored stream order; do not reorder by uncertain clocks or resettable sequences.

## Required contract, before any positive known-card output

One verified observer perspective applies to both hands; hand owner and observer are distinct. A positive card-name claim needs all of: supported observer mapping, exact source/entity-instance/epoch alignment, proof that the identity was legitimately available to that observer before the decision boundary, and supported continuity to the selected slot. Failure of any prerequisite means NOT ESTABLISHED IN DECISION-TIME EVIDENCE, never HUMAN DID NOT KNOW.

Private effective cost needs separate observer-availability and same-boundary cost evidence. Known identity does not establish known current cost. No printed-cost substitution, hidden modifiers or cost simulation. No final-catalog/opponent-list joins, later reveal/transform backfill, or reset/reconnect inference. Legitimately prior known information should not be erased solely by a HIDE marker, but neither may it survive a shuffle/change/identity reuse without supported continuity. First implementation need not infer such continuity; unsupported cases remain unknown.

## Recommended next bounded task — owner approval required

Read-only local evidence audit using existing pinned first-player, second-player and rewind fixtures (overlap is acceptable). No new capture campaign or uploads requested. Check existing raw records for:

1. Explicit observer/local-client mapping markers and whether they remain usable after resets/reconnects. Record absence as an evidence gap; do not derive identity from result or turn parity.
2. Actual SHOW/HIDE and repeated same-card reveal ordering, distinguishing private source knowledge from supported observer availability.
3. The ordering of actor identity disclosure relative to the selected BEFORE_ACTION boundary, including any preceding reveal records in the same action.

Deliver a small supported/unsupported evidence matrix with fixture hashes, source/epoch/raw-line references, alternative interpretations and exact unknowns. Keep raw private content/account data local; repository notes contain sanitized conclusions only. Reuse already retained raw evidence; no historical artifact rewriting or producer/schema change.

Audit acceptance is an evidence inventory, not a guaranteed positive mapping or resolved defect. If the existing artifacts lack the required proof, report that and propose the minimum separately approved capture-metadata change; do not manufacture a verified flag or broaden into a general knowledge engine. Only after this audit should a positive knowledge-gate API/schema or runtime integration be proposed.

Future implementation regressions must cover one observer across both hands, both first/second perspectives, action-trigger reveals excluded from earlier knowledge, appended later evidence invariance, repeated same-card reveals, legitimate prior knowledge, unknown capture versus human ignorance, private costs, conflicting mappings, source transitions, rewinds/entity-ID reuse and legacy missing proof. Existing reviewer source/cutoff tests remain nonblocking suggestions, not a new priority assignment.

## Gates retained

Existing254 ordinary/11 private passes and independent position-only review are previous evidence, not rerun in this contract review. No observer knowledge or universal HSC closure accepted. Canonical backlog reconciliation still requires its specific owner write approval; this review neither refreshed nor edited the canonical document. No external Resource Exchange writes in this review-only turn. Implementation, source publication, APK/phone validation, merge and deployment remain separate.
