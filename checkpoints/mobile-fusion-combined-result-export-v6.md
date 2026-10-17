# Mobile Fusion combined result/export validation v6

Date: 2026-10-17 Central

## Included source

- Plain-language incidental fusion consistency notices.
- Current-session-first automatic discovery with explicit next-three expansion and a four-session total cap.
- Corrected-result save/export race fix: the edit dialog remains modal until persistence finishes, and manual export reloads the current saved record by match ID.

## Verification

- Source and remote branch match `9c58860b11179b71b2f852dd9fc1afa9930743fd`.
- Full ordinary core suite: 215 tests passed.
- Android `compileDebugKotlin` and `assembleFusionTest`: passed with SDK Platform 36 revision 2 and isolated output.
- APK: 68,310,489 bytes; SHA-256 `8d90cc3f2d966314b4ad8773505db8cb21510715bff95842f4b404d7f1352e68`.
- Package/version remain `com.stroexd.hsdecktracker.fusiontestr4`, `1.5.0-fusiontestr4` / `10500`; min/target SDK 26/36.
- Signer certificate remains `4cf267d92ed361c32e2d4baa302670ee6c62a0cad042586e43de684ccc18f380`; permissions match the accepted baseline.
- `apksigner`, `zipalign -P 16`, and all eight native-library 16 KiB checks pass.
- REA/JADX static inspection reports 22,556 classes and 553 resources with the expected Fusion coordinator, codec, validator, reducer/stitcher, discovery, presentation, and coaching-summary surfaces. Summary parent Evidence `ev_b10f6bc538c75bc17cfe3aaece3466582096fe03a0ded764b2d276252d992310`; Fusion class-search Evidence `ev_2663017402a5b7f057feec8251fdf7f8daf9c0e0ee5f3753b675f13ea1616a85`.

## Delivery and remaining gate

Drive delivery folder: `H:\My Drive\HSReplay\Development\mobile-fusion-r5-combined-result-export-device-validation-v6`.

Install as an update without uninstalling or clearing data. Primary phone acceptance requires an existing fused match to be edited to the opposite result, persisted, and exported as current revision 2 with one `USER_EDIT` correction while the prior fused artifact remains separately available. Automatic current-session/next-three discovery is a secondary bounded check when a suitable unfused saved match exists; no new game is requested solely for validation.

Build and static evidence do not establish phone behavior. No merge, deployment, R6/background collection, or existing-artifact rewrite.
