# Independent candidate-association review — 2026-10-20

Outcome: no blocking issue found under the stated trusted adapter-derived source/document contract. This is static source review, not runtime/phone acceptance, raw-file authentication or verified observer knowledge. No reviewer edits or test execution. Coordinator's existing261 ordinary/12 private passes were not independently rerun.

Scope: ObserverAssociation.kt, ObserverAssociationTest.kt and relevant parser/adapter/reducer source. Private fixtures and raw reports excluded. Branch feature/mobile-fusion-offline, HEAD c28ad9d19ff5c8a90e4b41fb8aa0809fa49b6fd6. Verified SHA256: source f87c7d2454567dc171c163c9fc50fa1501a6319176dd9f1db2c6f0e1839d799b; synthetic test31bf23844b9e7ef433bdafe0d7971557b4c56952d21fec261d329dc49de1e4b1.

## Optional hardening — implementation priority UNASSESSED

1. ObserverAssociation.kt:65 skips missing/wrongly typed choice.evidence. Append {"choice_id":"2","submitted_log_time":"t","submitted_entities":["9"]} to a valid synthetic document: existing candidate2 remains despite the unlocated additional submission. Returning UNKNOWN would strengthen defensive behavior. Optional under current trusted-parser contract: PowerParser.java:34–40 initializes evidence and records recognized submission headers; aggregate submission fields accompany it.
2. ObserverAssociation.kt:64–71,103–105: future-evidence invariance applies to valid located adapter output, not arbitrary malformed appended metadata. Appending {"method":"SendChoices","raw":"id=2"} without source_line to evidence changes earlier cutoff10 from CANDIDATE to UNKNOWN; null choices/mappings also refuse. Safe refusal, not false knowledge. Clarify contract and add focused synthetic tests before widening inputs.
3. Reset recognition agrees with normal parser-produced blocks, but shortened raw BLOCK_START BlockType=GAME_RESET Entity=0 is accepted by resolver while the parser's full block pattern would not supply block_type, so reducer epoch would not advance. Malformed-log compatibility hardening, not demonstrated ordinary-output defect. Consider structured block metadata alignment.

## Confirmed boundaries

Cutoff/epoch filters exclude pre-reset offers/mappings and future associations. Repeated IDs stay ambiguous rather than selecting latest; exact duplicate headers are harmless. Conflicting aliases/offers/controllers do not return a candidate. Outputs omit private aliases; cancellation propagates. Candidate does not prove capture ownership, human visibility or decision-time card knowledge. Names/private costs remain withheld.

Owner authorized review only and raised five-hour ceiling to75% (supersedes60%). Latest check52%five-hour/9%weekly, no reset. No source corrections, tests rerun, commit/push, APK, runtime integration, canonical backlog change, merge or deployment. Recommended next gate: bounded defensive hardening and focused tests, separately approved; source publication remains separate.
