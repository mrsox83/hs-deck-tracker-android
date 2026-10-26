# First-player recognition - source-local validation

Owner07:40 UTC approved source-only publication of this sanitized checkpoint and the three reviewed source/test files. Validation below remains the pre-publication receipt; source commit/remote/shared receipts belong in current coordinator records. FIRST DELIVERED none; no APK/phone acceptance granted.

Owner07:20 UTC approved focused synthetic reproduction, minimal recognition fix and independent review. Basefaf1fbaa5e9fb1309472dcf4e4ca7ffe50d00f84/feature/mobile-fusion-offline, existing checkout preserved. SOURCE-LOCAL PASS; NOT PUBLISHED, NOT IN AN APK, NOT PHONE-ACCEPTED. FIRST DELIVERED none. No app/schema/pairing/build-input changes, commit/push/merge/signing/install or raw/coaching/statistics changes. Earlier saved-edit APK and bounded phone acceptance remain separate and immutable.

## Reproduction and correction

Both synthetic assertions FAILED against unchanged original production (2 tests/2 failures,39s): a Coin popup before first observed turn incorrectly established second-player; a recognized opening followed by missed actual turns incorrectly treated first observed enemy button as starting order. Original XML/console evidence .gradle/first-player-20261026/before and before.log; no private fixtures or phone reproduction claim.

VisionGameTracker now excludes ordinary Coin card/popups and first-observed turn buttons from starting-order evidence. Only exact normalized existing extra-card opening message in header region, explicit mulligan context, no turn/button/banner, before mulligan confirmation and within existing60s opening window can accumulate evidence. Two distinct consecutive observations within2s establish second-player when mulligan ends. Interrupted/gapped/duplicate-time/late/nonopening/left-side/substring observations remain UNKNOWN. Turn counting/card recognition remain independent; no new parser/engine/schema/interface or pairing exception.

Automatic TRUE recognition deliberately becomes UNKNOWN: absence of Coin or first visible own turn is not proof that the player started. Existing explicit controller/manual TRUE/FALSE values and fresh/reset UNKNOWN retained; later generated Coin cannot overwrite them. Reduced automatic coverage is intentional. Exact opening region/two-frame conjunction is synthetically supported, not phone-layout/runtime validated. No hidden-card/turn-parity inference or automatic rewriting of saved records/fusions.

## Verification and independent review

| Gate | Result |
|---|---|
| Failing-before |2/2 assertion failures, original production39s |
| Focused + adjacent |77 tests/5 suites PASS56s; FirstPlayerRecognitionTest, VisionTest, TrackerTest, FusionCoreTest, PostMatchDiscoveryTest; source/test compile and test tasks executed |
| Final ordinary :core:check |333 tests/53 suites,0 failures/errors/skips PASS20s; compilation FROM-CACHE on same inputs, test task executed. No ordinary private fixtures. No separate lint task configured in core; check and git diff --check passed. |
| Independent reviewer | Separate first_player_review, read-only source/design/test inspection, no blocking issue. Reviewer flagged stale-mulligan risk; fixed with unconfirmed/context/time-window gates before final tests/review. Optional concurrent turn/banner assertion coverage noted, nonblocking; code explicitly excludes both, own-turn existing exact-event fixtures retained with UNKNOWN expectations. |
| Final hashes | VisionGameTracker.kt f28eaec1192ef26cdd680123ac22980d5c28d59b6d2e1f81fb2577bc7d179b6a; FirstPlayerRecognitionTest.kt15be14c8070e8df76ec2e9b6cee4c6f96f6570b4341ea22f1134dedf59b1facd; VisionTest.ktc7ab980131997a07fcac36d2c9c7494f44ea311b00eea70e1a0b198497368e9b. All match independent review and final tested bytes. |

Production diff only VisionGameTracker.kt; new7-case FirstPlayerRecognitionTest; two existing VisionTest event expectations now omit unsupported automatic TRUE. Existing controller explicit TRUE propagation and pairing conflict suites pass. Final logs/XML/init helpers .gradle/first-player-20261026/{before,after,final}, console before.log/after.log/final.log. No APK compilation/packaging/signature/REA/device gate run or claimed.

## Limits and next step

HSC-004/009/010 broadly OPEN, PRIORITY UNASSESSED. Local historical evidence audit is excluded from sanitized publication; exact phone cause/recognition frames/search trace/intermediate edited-record bytes remain unknown. This source correction prevents demonstrated synthetic overclaims, not proof of original incident cause, general recognition correctness or universal recovery. Saved-note projection/provenance schema extensions, general turn-counter repair and historical backfill remain separate.

Next owner decision: approve source-only publication of exactly these three reviewed source/test files plus this sanitized checkpoint, with existing Resource Exchange/canonical evidence reconciliation under BUILD_NOTES_STANDARD. No publication or APK authority inferred here. Shared HSC-004/009/010 source-local finding reconciliation explicitly PENDING that coordinator publication/review scope, not an access denial; old external broad issues must remain open. No new game needed now. Later APK preparation/checks/Drive delivery and bounded phone gate need separate approval. No active implementation worker or reservation after closeout; unrelated dirty instructions/docs/reports/helpers/private evidence preserved.
