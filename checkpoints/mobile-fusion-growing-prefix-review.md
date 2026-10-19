# Growing-log prefix characterization — 2026-10-19

## Current implementation — owner-approved bounded correction

Subsequent owner-approved fresh-ID follow-up is implemented locally under tracker-record/1 with /4 producer defaults. Its tests/review and non-overwrite/legacy-preservation gates are recorded separately in checkpoints/mobile-fusion-stable-game-id.md. Older statements below that fresh IDs still differ describe the completed earlier candidate-only unit; they do not describe this follow-up's current policy. No historical artifact migration or APK is implied.

Independent read-only review completed on2026-10-19 by local subagent growing_prefix_review: no blocking findings. Production/test patch was frozen during review; reviewer inspected source and saved reports, did not rerun tests or inspect private Drive/raw. Android runtime and separate logical-ID policy remain outside acceptance. Full review and next-policy recommendation: docs/coordination/reports/GROWING-PREFIX-REVIEW-2026-10-19.md. No source publication or APK implied.

Duplicate-candidate correction is implemented and locally verified: 237 ordinary core tests across39 suites and all10 private RealFusionFixtureTest checks pass with zero failures/errors/skips. Final combined Gradle invocation succeeded in1m39s. Reports remain under .gradle/growing-prefix-fix-verified/core/test-results/{test,realFusionFixtureTest}. Existing source IDs, serialized source format, fused logical-ID policy and import immutability remain unchanged. No APK, publication or phone acceptance.

Exact growing-log replay now accepts DK with early/late exports separately and together in either order, chooses the same representative in both combined orders, and passes the conservative discovery gate. All three original Power sources plus tracker remain in the artifact, with one equivalent-source audit diagnostic and a one-source partial warning for the separate Priest game. Artifact provenance validates. Reimport preserves one existing entry and identical saved artifact bytes. Priest remains PENDING. The private inventory still contains21 bundles/48 matches,43 completed/4 truncated,21,018 canonical events/29,674 snapshots. Current diagnostic counts also match reduction with ingestion equivalence disabled for every inventory match. Logical IDs across separate early/late assemblies still differ by design pending separate policy review; this patch does not claim that issue resolved.

Bundle ingestion computes an ephemeral completed-prefix/1 key only for completed, explicitly non-truncated evidence whose document-declared raw hash matches the independently hashed enclosed Power.log. It compares the canonical full parsed document except its verified whole-log hash, including session, choices, identities/mappings and source locations, plus an exact raw-byte prefix through the highest referenced source line. Keys are never loaded from serialized evidence and are stripped from the assembled artifact model. A 32KiB buffer streams the additional prefix scan; boundary tracking uses constant scalar state per match rather than duplicating the raw log or retaining every line number. Cancellation/deadline callbacks cover the scan and canonical traversal. Unsupported SHA-256 digest cloning disables equivalence conservatively; Android-provider/runtime behavior remains a future phone gate.

Pairing and continuation evaluation use deterministic representatives only for these verified equivalent completed matches, while evaluation.powers and assembled sources retain all originals. Saved diagnostics explain each additional equivalent source; unrelated sources still generate the existing partial-reconstruction warning. Missing proof, different sessions, changed parsed content/raw prefixes, incomplete and truncated evidence stay separate. This is not reconnect stitching or universal identity recovery.

Historical private-corpus golden audit: the broad 10-test run exposed a diagnostic-count expectation predating published /3 reset chronology (400 unresolved-owner warnings versus current437, with rewind/reset epoch diagnostics absent from the old golden). Reducer source is unchanged in this patch. The corpus now explicitly compares diagnostics with ingestion equivalence disabled for every inventoried match before asserting the current exact counts; no gameplay evidence or parser output is rewritten. The initial broad run was 9/10 private tests passing and is not reported as all green.

Windows execution notes: sandbox native-platform.dll startup failed, so verification used the authorized host-side cached offline runner. The first reused isolated report directory was locked; fresh directories preserved all prior outputs without deletion or process termination. Final reports above supersede interim runs. Temporary routing init script removed after verification. Live usage35% five-hour/45% weekly near final validation, ceiling90%, no reset consumed. Changes remain local/uncommitted for review.

Canonical HSC-004/010 reconciliation remains pending the owner's review of the prepared canonical-write proposal. Old wording reports repeated-prefix risk; new bounded evidence reproduces and locally corrects candidate ambiguity, not duplicated live phone history. The connector is not the remaining write blocker. Broader logical-ID changes, Priest pairing corroboration and hand reconstruction remain separate.

## Historical investigation and design

Owner approved queueing a separate confirmed close-Hearthstone convenience action and continuing the identity investigation. Force-stop is not required to make normal multi-game log sessions correct. No production change or APK in this investigation.

Exact private inputs are pinned by wrapper and tracker hashes in growingLogPrefixIdentityCharacterization. Early Power.log SHA7b35adfe... contains one completed DK game; later SHA6399c24e... contains the identical nested match1 plus match2. Raw source identities differ because they include the entire growing file hash.

Observed current-core behavior:

- DK with early ZIP alone ACCEPTED; with late ZIP alone ACCEPTED.
- DK with both ZIPs CONFLICTED in either selection order. This reproduces false candidate ambiguity from equivalent game evidence, not duplicate live phone history.
- Fused logical IDs differ between separate fresh assemblies with early versus late selected sources.
- An isolated repository import followed by reimport through the same tracker record retains one entry and the original immutable fusion. No duplicate entry was produced in this path.
- Priest with both ZIPs remains PENDING; this is a separate observation requiring its own diagnostic interpretation, not asserted as fixed or necessarily caused by duplicate DK evidence.

The single private characterization test passes by pinning these current observations; PASS means reproduction is stable, not that desired deduplication is implemented. Initial expectation of Priest ACCEPTED failed and was replaced with the measured pending state to avoid a false acceptance claim. Exact report is retained under .gradle/growing-prefix-review-2/core/test-results/realFusionFixtureTest locally; private raw sources remain unchanged on Drive. Temporary output-routing script removed.

Next proposed correction: recognize exact equivalent completed-match content within verified source lineage while retaining every original raw hash; avoid changing history merely because an unrelated later game enlarged the source. Preserve genuinely distinct games and explicit ambiguous cases. A fix, migration policy and complete regression suite remain to be reviewed; no production modification, commit/push, merge or deployment is claimed here.

## Follow-up source review — implementation boundary

Owner resumed this investigation after the agent-usage discussion. Branch remains feature/mobile-fusion-offline at3117f992c249e630490a14b2bacf460c42f00072; existing five dirty paths preserved. Live usage26% five-hour/43% weekly, ceiling90%. No new agent setup or worker launch.

Three distinct responsibilities need correction or preservation:

1. Evidence identity remains raw-source-specific. Do not simply replace FusionSource.id with a hash of parsed match JSON: evidence references, validation and raw-source retrieval depend on knowing which original source supplied a claim. Same-session filenames or matching outcomes alone do not establish equivalence.
2. Pairing needs an explicit equivalence layer before scoring and continuation evaluation. Collapse only completed, non-truncated matches with exact matching parsed content and independently verified raw lineage/covered bytes. Retain every original source and hash as provenance; select a deterministic representative for timeline evidence. Missing proof, changed content, incomplete matches and different sessions remain separate. Compare complete match content, including choices and identity information, not only action counts or endpoints. Equivalence is not reconnect stitching.
3. Fresh logical identity must not include unrelated selected provenance. Coordinator currently hashes tracker UUID plus every retained source ID. Use a separately domain/version-tagged logical identity policy anchored to the saved tracker identity; review whether accepted evidence identity belongs in that key before implementation. The existing importer treats tracker UUID as the immutable ownership boundary already. Preserve that guard and never rename/rewrite/backfill existing saved artifacts automatically.

The first patch should address exact-equivalent candidate ambiguity with retained provenance and regression tests; logical-ID policy is a separate reviewed decision, not a silent side effect of deduplication. This avoids combining a pairing correction with historical identity migration. No public model/interface, dependency, persistence format or source-ID change is required merely to define the equivalence proof.

Required regressions: early+late in both selection orders yields one viable DK candidate; independent Priest game remains present; unrelated same-result games, changed choice/entity content, different sessions, missing lineage proof, incomplete/truncated sources and genuine continuation fragments never collapse. Repeated import preserves original file bytes and one entry. Private Priest PENDING is not changed to ACCEPTED without separate corroboration. Existing reports/tests are reproduction evidence only; no fix acceptance yet.

Next implementation prerequisite: retain an independently computed raw-lineage proof at bundle ingestion, with exact boundaries and cancellation/memory bounds, rather than trusting unverified JSON metadata. Review its propagation through reduction and provenance retention before modifying evaluator selection. No APK is needed for this core unit.
