# Mobile Fusion REA shipped-artifact gate

Established 2026-10-15. This gate independently checks what a built APK actually contains. It supplements source review, Gradle tests, package/signature tooling and physical-device validation; it does not execute the APK or establish runtime behavior.

## When to run

Run for each APK promoted to a phone-validation bundle or release candidate. Do not repeat it for an unchanged APK hash. Use the installed `reverse-engineer-anything` Android workflow and the connected server's advertised schemas.

## Required record

1. Record the exact APK path, size and SHA-256.
2. Run `inspect_android_package` with summary detail and retain its parent Evidence ID.
3. Record REA/JADX identity, package name, version code/name, minimum/target SDK, manifest status, permission list, class/resource counts and all reported limitations/unknowns.
4. Search the compiled `com.stroexd.hsdecktracker.core.fusion` namespace. Record the match count and confirm the expected critical surfaces for that build, such as coordinator, artifact codec, provenance validator, reducer/stitcher and any authorized discovery or collection components.
5. Compare package/version/permissions and critical-class presence with the previously accepted APK. Treat additions, removals or unexplained drift as a review finding, not automatically as a defect.
6. Use focused class/method inspection only to answer a concrete packaging or source-versus-artifact question. Do not bulk-decompile the application as a routine gate.
7. Continue to verify signatures/alignment with the existing Android tooling; REA's Android provider does not verify APK signatures.
8. Preserve the distinction between static shipped-artifact evidence and phone/runtime evidence. Shizuku authorization, Binder recovery, overlay usability, SAF/Drive behavior, lifecycle continuity, recognition and duplicate prevention still require their existing tests or device gates.

## Initial baseline

Artifact: `C:\Users\Xs_da\Documents\SoxCoachExporter\Matches\mobile-fusion-r5-manual-export-device-validation-v4\app-fusionTest.apk`

- SHA-256: `527b74cb4ce7be1ffe6bc98ca73faafa6f32c6862ff70db24cd664d60ca044b3`
- Package/version: `com.stroexd.hsdecktracker.fusiontestr4`, `1.5.0-fusiontestr4`, code `10500`
- SDK: minimum 26, target 36
- Inventory: 22,553 classes, 552 resources, 95 classes matching `com.stroexd.hsdecktracker.core.fusion`
- Android summary parent Evidence: `ev_b9ec01694b1a308741a6bee983af47e218f6d976cb4266708dbcac210ae23d84`
- Fusion class-search Evidence: `ev_93ceaeb979d8430d85305bac15493034151c4e67ca1bbd5a44889fd6b80d2bfc`
- Provider: `jadx-headless-mcp` 0.7.1, artifact SHA-256 `6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75`
- Signature verification: not performed by REA; retain the existing apksigner/alignment gate.

The manifest includes overlay, MediaProjection, foreground-service, notification, Shizuku, network and inherited package-install/update declarations. `REQUEST_INSTALL_PACKAGES` and `UPDATE_PACKAGES_WITHOUT_USER_ACTION` require explicit provenance/necessity review before broader distribution; their presence alone does not prove use or misuse.
