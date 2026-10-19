# Fresh fusion identity — 2026-10-19

Owner approved this bounded follow-up after growing-prefix candidate review. Branch feature/mobile-fusion-offline, baseline3117f992c249e630490a14b2bacf460c42f00072, preserving pre-existing local changes. No APK, old-history rewrite, source publication, merge or deployment.

## Policy and compatibility

Fresh assembly uses tracker-record/1 identity: first128bits of SHA-256 over UTF-8 `logical:tracker-record/1`, NUL separator and the exact saved tracker ID. This preserves the existing32-lowercase-hex filename/lookup shape. Added/reordered/changed selected sources, result/revision edits and content timestamps do not change the new identity. Different saved tracker IDs remain separate; no attempt to identify duplicated real games across different saved tracker records is made.

Saved IDs are opaque to retain compatibility with non-UUID legacy imports. Blank, padded, control-containing and malformed Unicode IDs fail explicitly rather than inventing/normalizing identity. Valid supplementary Unicode is retained. The read-only reviewer identified UTF-8 replacement of lone UTF-16 surrogates as a collision; explicit encoder validation and rejection/distinct-valid-Unicode regressions address it.

New default offline producer offline-r2/4 and importer android-r3/4 identify this source change. Schema remains hs-fused-match/1, source IDs/raw hashes remain unchanged, archives/provenance are retained. Required entity-epoch validation applies to producer suffixes>=3 so /4 cannot bypass /3 safeguards. A saved diagnostic names tracker-record/1. Explicit older-version fixture decoding remains supported; no bulk backfill, filename migration or old-artifact regeneration.

## Storage preservation

Importer still returns the existing FUSED_LOCAL record before assembly. The low-level artifact writer now validates the32hex destination, serializes writers across repository instances in the Android app's single-process boundary, and compares existing target bytes with the encoded staging file. Identical retries return the existing path without rewriting its modification time. Different content at that ID fails explicitly and preserves the original, including a hash collision or unjournalled/orphan artifact conflict. No automatic revision workflow is introduced. In-memory byte comparison uses two32KiB buffers with cancellation checks; staging cleanup remains finally-protected.

An unchanged atomic rename is used only when the destination is absent; replacement is not requested. This is not a guarantee against a second process/external writer racing the private directory, and no cross-process write workflow was found/added. A conflicting orphan may require an explicit owner-approved recovery/revision workflow; do not silently overwrite it to make retry succeed.

## Validation and review

Four new ordinary checks cover source/result-independent IDs and different records; invalid/Unicode identities; identical/different-target preservation across repository instances and invalid paths; /4 round-trip and archive enforcement. Existing private growing-prefix replay now requires equal fresh IDs across early/late/combined sources and seeds a legacy-named /3 fixture, then confirms its exact saved bytes/path/outbox entry survive edited-record/longer-log reimport. This is a synthetic legacy-file preservation gate using pinned raw inputs, not a claim to have modified or retested the installed phone's historical storage.

Final corrected-code verification passed241 ordinary tests across39 suites plus all10 private fusion checks, zero failures/errors/skips, combined Gradle run1m40s. Reports: .gradle/stable-game-id-verified/core/test-results/{test,realFusionFixtureTest}. Exact early/late fresh assemblies both produce32c52d8e4566120ba4931f8bc976c82e; combined selection shares that ID. Legacy-named fixture path/bytes/entry preservation passed, and separate Priest pairing remains PENDING. Initial combined run also passed but predates the final Unicode validation and is not the final evidence. Reviewer found no other blocker and did not rerun tests or inspect private raw/Drive. Android runtime/provider behavior remains a phone gate, not established by JVM tests.

Final read-only follow-up confirms encoder validation fixes the identified collision and tests cover lone high/low surrogates, valid supplementary Unicode and the pinned .NET-independent tracker-match ID7f8a574011965aa9d7bd81abf66055e6. No remaining blocking findings from inspection. Test execution is still the coordinator's separate gate, not a reviewer rerun.

Canonical HSC-004/010 reconciliation remains pending owner review of the earlier concrete canonical-write proposal; local source correctness is not universal issue closure. Resource Exchange patch notes must list this only as SOURCE-LOCAL until a separately authorized APK is delivered. No standalone APK for documentation or this local verification.

Resource Exchange patch notes were updated in place with /4 fresh-ID behavior and241/10 verification, explicitly SOURCE-LOCAL/no new APK. Last successful live usage54% five-hour/48%weekly against90% ceiling; final refresh returned unavailable, not zero usage. No reset consumed. Source changes remain uncommitted/unpushed. Temporary stable-game-id routing script removed after final verification; pre-existing unrelated scripts/artifacts preserved.
