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
- `:app:assembleFusionTest`: PASS with isolated build output. APK SHA-256 `c2fa1d59a483302e07febc92d9c6521a979cd35de36a8532c40b90c134b7bc9f`, 68,309,161 bytes.
- Packaged identity remains `com.stroexd.hsdecktracker.fusiontestr4`, version `1.5.0-fusiontestr4` / code `10500`, min/target SDK 26/36, and the established signer certificate SHA-256 `4cf267d92ed361c32e2d4baa302670ee6c62a0cad042586e43de684ccc18f380`. Permission inventory matches the accepted baseline.
- `apksigner`, `zipalign -P 16`, and `scripts/check_16kb_alignment.py` all pass; all eight packaged native libraries report 16 KiB alignment.
- REA/JADX shipped-artifact inspection observes 22,556 classes, 553 resources, complete Fusion-namespace coverage including `FusionCoordinator`, `FusionArtifactCodec`, `FusionProvenanceValidator`, `PowerEvidenceReducer`, `PowerContinuationStitcher`, discovery/collection classes, and the new `PostMatchPresentation`. Package search Evidence ID `ev_a4b41ab27a9b45b47563ae1be05b7648b6e9d1cf8c1f07be26ee035c8811f0bd`; focused class Evidence ID `ev_abae1a425af413fca330f5cca69ef228407b478af263ce50b5b9ae9801a8f445`.
- The +3 classes / +1 resource versus the prior baseline are bounded build drift consistent with this source unit; no package, SDK, permission, or signer drift was found. REA is static evidence and does not establish runtime behavior.
- The exact APK, checksum, and bounded checklist are staged at `C:\Users\Xs_da\Documents\SoxCoachExporter\Matches\mobile-fusion-result-conflict-device-validation-v1`.
- The owner-accessible Drive package is `H:\My Drive\HSReplay\Development\mobile-fusion-r5-manual-export-device-validation-v5`, following the established one-versioned-folder-per-build convention. Its APK is reread and hash-matched after copy.

## Boundaries

- APK is built and staged locally and in the verified Drive package for owner phone validation; it is not phone-accepted, merged, or deployed by this checkpoint.
- Phone layout/runtime behavior remains unverified. Install as an update only; do not uninstall or clear app data.
- Legacy fused entries deliberately report result agreement as unverified rather than inferring it.
- No private fixtures, merge, deployment, R6/background collection, or existing-artifact rewrite.

## Phone observation and queued wording follow-up

- Owner screenshot `9530.jpg` confirms Android updated the intended existing test app and retained its data boundary.
- Owner screenshot `9531.jpg` confirms the legacy/unverified warning appears in **Game logs & export**. The warning behavior is present, but its provenance/revision wording was too technical for an incidental user notice.
- Source now uses plain-language titles and messages for current, edited, result-changed, and older-fusion states. Detailed failure/error text remains unchanged for troubleshooting. Focused `PostMatchPresentationTest` passes using isolated output.
- Per owner direction, this wording-only source change does not trigger a new APK. It will be included in the next otherwise-needed build; the installed v5 package still contains the older wording.
