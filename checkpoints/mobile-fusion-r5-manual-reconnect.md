# R5 manual-record reconnect — bounded diagnosis, 2026-10-12

Owner authorized diagnosis and a defect fix or honest partial warning. Context HSC-004/HSC-009 (reconnect/journal), HSC-013 (coaching correction). No R0-R2 discretionary expansion, automatic collection, R6, merge or deployment.

## Exact evidence and result

Drive ZIP fusion-test-f2af39db-b51d-4bdd-a43a-5fe2477fa12d-a1810e19-2459-4f58-a709-5093a5ca32d6.zip:
SHA256 b688245ea65034ff967762ecbc44b0c047ed996437f663a77f4e7aa5f1909419.
Tracker match_20261011T230556258Z_515686c3ce11554b26c6c709e3bffa8939c403111c16f115bcfd6642ef8ce40a.json:
SHA256 5b9101309a2cf21622387adadc885d37513c30b71fcdb625e64694a4af398676.
Tracker ID f2af39db-b51d-4bdd-a43a-5fe2477fa12d, MANUAL, empty timeline, Death Knight/Priest LOSS.
Sessions Hearthstone_2026_10_11_17_55_01 (incomplete) and Hearthstone_2026_10_11_18_02_04 (completed), neither manifest source-truncated.
Logical ID 0102e83964067daec883a187d0fa8eb9. Later source pairs with 57-second endpoint difference and matching result. Its 172 events form the current fused timeline. Earlier source is catalogued, not stitched. Do not treat source count as reconstructed coverage.

The exact private gate reproduces two continuation rejection reasons: no unique open-choice continuation anchor; raw turn continuity is absent. Existing narrow stitch contract is behaving as specified, not proven broken. Do not remove either guard merely to accept this match. General reconnect support is a separately scoped future enhancement requiring independent identity/ordering evidence and negative tests.

## Implemented warning

Coordinator now warns when an accepted timeline leaves original selected Power sources unincorporated. Warning explicitly permits unrelated games as well as unverified reconnect fragments; it does not assert they belong to the accepted game. Accepted stitched parents do not trigger the warning. No pairing, reconstruction or stable logical ID change.
Persist coverageWarnings in the import journal with backward-compatible empty default; show on collection/retry, dialog reopening and game details. Artifact diagnostics carry the warning. Existing old fused artifacts/journal entries are not migrated or regenerated automatically; this phone artifact remains unchanged. Bundle into next needed APK, no standalone build requested.

## Validation and boundary

169 ordinary tests pass, zero failures/errors/skips. New hash-pinned private manual reconnect gate passes and verifies accepted later-only timeline, unchanged logical ID, provenance, warning and journal reopening. Prior pinned reconnect parser/journal gate passes. Android fusionTest Kotlin compilation passes. Initial test compile ByteArray/String mismatch corrected and rerun successfully. No private fixture in Git, original files unchanged.

Missed automatic tracker record: owner-observed, cause not established. Both capture backends feed the same ScreenRecognizer/OCR/VisionGameTracker. Recognition source stop/start resets the recognizer; idle timeout is four minutes; automatic completion requires repeated result recognition while in a game phase. Missing capture or game/result recognition can prevent saving, but no frame trace for this failure has been proven. Do not claim MediaProjection is inferior or change capture policy from this one observation. Next reproduction, if needed, requests recognition diagnostics around reconnect/end, not another destructive five-game interruption.

Coaching: campaign remains eligible by owner override, but this timeline is partial. Raw fragments are preserved; do not count as two competitive games or infer unobserved actions. HS-COACHING owns ledger reconciliation; no ledger write/analysis claimed. Other uploaded campaign IDs await separate validation.

Live usage at start 0% five-hour /5% weekly; after validation 6%/6%, ceiling90%, no reset consumed. Source-ready/tests-passed/Android-compiled; no new APK/phone-warning acceptance. Git/shared publication verified at handoff.
