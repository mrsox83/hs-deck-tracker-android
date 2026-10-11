# R5 report-wait preparation — 2026-10-11

Owner authorized readable session labels, explicit retry feedback, regression preparation and a five-game reporting checklist. Scope: HS-FUSION, existing HSC-004/HSC-009; no companion edits, automatic collection, R6, merge or deployment. Keep the owner's current manual-v1 APK for the campaign; no standalone polish APK is built or staged.

Preflight: local/remote starting tip `0623c073cb26ac837622011de41b5f21115b8d80`, clean `feature/mobile-fusion-offline`, mrsox83/hs-deck-tracker-android. All four tracker ancestor checks passed. Existing fusiontestr4 application ID/version/signing configuration unchanged. Initially H: was absent; after owner mounted Drive, START_HERE/current owned notes and the implementation plan were read. Canonical HSC status is not marked DONE or claimed reconciled.

Usage: first fresh sample 1% five-hour/0% weekly; later sample 7%/1%. Existing authorized five-hour ceiling90%; no reset consumed. Weekly percentage does not cap this unit.

Source preparation: strict session-folder date parsing displays a readable timestamp and retains the exact alias. Invalid/legacy names remain unchanged; no timezone or match membership is inferred. Selected game's stored timestamp is shown in phone local time with classes/result/deck. Already-fused retry feedback uses the actual preservation diagnostic from the operation result, not a speculative pre-call state; explicitly says no regeneration/new reader read/upload. Fresh fusion and pending/failed pairing remain distinct.

Regression additions: label/midnight/invalid-date cases, result-text distinctions, cancelled raw-only bundle regeneration after repository reopen with deterministic repeat/hash preservation, and permission/Binder-shaped read failures that remain failed journal entries without ZIP success. The latter tests only durable failure accounting, NOT Android permission or Binder callbacks. Repository regeneration is NOT end-to-end phone/importer interruption acceptance. Device gates and five-game reports remain pending.

Validation completed: 165 ordinary tests and one pinned private reconnect gate pass with zero failures/errors/skips; `:app:compileFusionTestKotlin` passed. No APK assembled/staged and no new phone acceptance claimed. Final usage8% five-hour/1% weekly, below90%; reset unused. Publication verified separately after commit/push.

Build recovery: wrapper attempt hit Java PKIX trust error downloading Gradle. Existing cached Gradle8.14.3/JDK21 used offline; a PowerShell unquoted property was corrected. Old generated core output was locked, so validation used fresh `build/r5-prep-validation` outputs through a local init script; prior outputs, sources, APKs and fixtures preserved. No certificate verification disabled. Non-blocking existing SDK XML/deprecation/overlay warnings remain. No source, fixtures or app data were deleted.
