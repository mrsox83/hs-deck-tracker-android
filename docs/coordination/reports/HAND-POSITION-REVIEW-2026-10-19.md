# Hand positional evidence — independent review

Date: 2026-10-19. Owner-approved fresh read-only review by positional_review. Branch feature/mobile-fusion-offline, HEAD4e9e7222c51a0572dff99c0569cb5e86a8f01b6e plus local uncommitted correction. No files changed by reviewer, no delegation, no private fixtures/Drive/raw gameplay/APKs/reports accessed, no tests rerun.

## Result

No blocking findings in the bounded position-only in-memory correction. This is not observer-knowledge acceptance, universal hand accuracy, APK/runtime acceptance, publication or canonical HSC closure.

Reviewed the reducer diff, DecisionHandView.kt, PositionalDescriptorTest.kt, DecisionHandViewTest.kt and supporting source. Raw-consistent complete descriptors import only positional fields; named and previously explicit tags retain precedence. Disagreements are summarized. Reset clears explicit-tag tracking with entity state. Projector checks source/cutoff/FULL transitions, uses the selected epoch archive, rejects conflicting latest-line proofs, requires three-field provenance, labels supplemental support and leaves completeness/identities/private costs unestablished.

Explicit precedence intentionally may retain an older explicit value despite a later conflicting descriptor. Disagreement disclosure does not establish which source representation is universally correct. The narrow private witness and synthetic tests are finite evidence, not a general replay guarantee.

Nonblocking test follow-ups, PRIORITY: UNASSESSED: directly exercise a source transition requiring FULL; a source cutoff moving backwards; and a later positional tag failing to certify an earlier snapshot. Reviewer found these safeguards in source; related epoch/ownership/identity tests exist. Suggestions do not authorize a new implementation task or displace the approved queue.

Coordinator-reported254 ordinary/11 private passes remain the executed test evidence. Reviewer did not independently rerun them. Review did not inspect private fixture source assertions or establish the broader known-card contract.

## Reviewed source bytes

SHA-256 pinned by coordinator immediately after review; paths relative to repository. No source edits occurred during review.

- core/src/main/kotlin/com/stroexd/hsdecktracker/core/fusion/PowerEvidenceReducer.kt:167ed4ec0ffb47757555d207ec9a21596ee10e5976203dfd85a6ef6939cdab7d
- core/src/main/kotlin/com/stroexd/hsdecktracker/core/fusion/DecisionHandView.kt:cc082e6def17c64b323cc445aa129a3eefe8e224cd6d4da8d7f47b7bbdbedde8
- core/src/test/kotlin/com/stroexd/hsdecktracker/core/fusion/PositionalDescriptorTest.kt:e65a59138de6238f6df126a4be02f4fe15dda3266ade6edebac9327bdcc06d5b
- core/src/test/kotlin/com/stroexd/hsdecktracker/core/fusion/DecisionHandViewTest.kt:bce5449d72fda5bee78e13ff3f3ea735643546e37697352a0a28548b84b3060c

Next gate: owner-approved source-only publication, or separately scoped further consumer work. No APK, commit/push, migration, merge or deployment performed by this review.
