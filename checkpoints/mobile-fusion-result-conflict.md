# Mobile fusion result-conflict/manual-shell checkpoint

Date: 2026-10-17 Central

## Bounded outcome

- Each newly fused artifact records the saved-match revision and result used at assembly time. Existing outbox entries remain readable with null legacy metadata.
- A pure consistency classifier distinguishes current, later non-result revision, explicit result conflict, and legacy-unverified evidence.
- Match details and the manual logs/export dialog surface the exact fused revision/result versus the current saved revision/result. A result-conflicted artifact is labeled historical and is never presented as current.
- Existing fused artifacts remain immutable and exportable for provenance. The current saved match continues through the revisioned match-export path; no old artifact is overwritten or silently regenerated.

## Validation

- Focused `PostMatchPresentationTest` and `FusionCoreTest`: PASS.
- Full ordinary `:core:test`: 214 tests across 36 suites, zero failures, errors, or skips.
- `:app:compileDebugKotlin`: PASS with the established cached Android 36 validation toolchain and isolated build output.
- Temporary init script and isolated build outputs removed after validation.

## Boundaries

- No APK was built or promoted.
- Phone layout/runtime behavior remains unverified.
- Legacy fused entries deliberately report result agreement as unverified rather than inferring it.
- No private fixtures, merge, deployment, R6/background collection, or existing-artifact rewrite.
