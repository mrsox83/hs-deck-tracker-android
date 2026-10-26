# Hero-class label correction — reviewed source

Owner approved bounded implementation/tests, one independent source-only review, local private replay and subsequent source publication/shared reconciliation. Base eed36397738cca05c06d2bb456052b2f6f020efd, feature/mobile-fusion-offline. HSC-003/004/009 additional evidence; priority UNASSESSED, provisional severity S2. Broader issues remain OPEN.

## Problem and change

Generic card-name cleanup could turn numeric-modified gameplay text into a class word. Broad lower-screen geometry and early-game class updates then allowed it to replace the starting hero class. A later diagnostic session directly reproduces this mechanism; an earlier unrecorded mismatch has no exact trigger proof.

Class labels now have their own normalization path, reject numeric modifiers, and require bounded left/right opening nameplates plus paired-label or mulligan context. Turn-button/banner/result context excludes gameplay. Playing-phase OCR does not update classes. New games still accept fresh opening class evidence; midgame startup may remain UNKNOWN rather than guess. Card-name matching, controller/persistence, schemas, package identity and historical artifacts are unchanged. No migration/backfill.

## Validation and review

| Gate | Result | Evidence and limitation |
|---|---|---|
| Ordinary core suite | PASS | 314 tests /51 suites, zero failures/errors/skips; offline Gradle8.14.3 final run14s |
| Targeted regressions | PASS | Six tests cover modifiers, recorded-form text/coordinates, retained deck/state, opening context, multilingual/aspect/geometry bounds, numeric card-name recognition, new-game switch and midgame UNKNOWN preservation |
| Independent source/synthetic review | PASS | class_label_review, read-only, no private evidence or delegation; three coverage suggestions added and re-reviewed with no blocker |
| Private JVM session replay | PASS | 1959 frames:135 baseline events versus134 corrected; exactly one false class event removed, every other event/timestamp identical under pinned catalog/context |
| Android Kotlin compilation | PASS | Executed before test-only coverage additions; production hashes unchanged |
| APK/static/delivery/phone | NOT RUN | No new APK prepared or delivered; source checks do not prove device-layout acceptance |

Private replay executes a symbol-renamed baseline tracker from the exact base Git blob and corrected tracker against the same unchanged matcher/catalog. It is not a recreation of every phone runtime setting, persistence state or recorded log line. Private logs/images/catalog/harness stay outside Git and reviewer context. The original unrecorded trigger remains unknown.

Existing Windows native-library sandbox/output-lock failures were preserved; validation used the host and fresh ignored output directories. No destructive cleanup or test suppression. Existing compiler warnings remain. git diff --check passed.

Reviewed/tested SHA256:

- Ocr.kt: fd04348058b898ae9ff81a3e0ce9b8bf0731c67e653712b234d8a025070b4fdd
- VisionGameTracker.kt: 9e041447a6001d6ca52bf7516862172604098e7ee2bf8fcb7a414a0a3d440259
- VisionTest.kt: 9c21cf7fc6445bdee9703a98911517dd7f8a8b140ea19476c74e433bc2b9fa1d

Source publication and shared readback receipts belong in the dated publication checkpoint. Existing /8 card-attribution phone acceptance is retained; this new class-label correction is not delivered or phone-accepted. No merge/deployment or old-record rewrite.
