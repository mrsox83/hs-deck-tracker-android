# Tracker capture setup shortcut — 2026-10-11

Owner-authorized small UI follow-up from screenshots9339/9336. Tracker's OpenInNew toolbar action opens the exact BackgroundSetupDialog used by Settings rather than directly launching Hearthstone. Its content description now names background setup. Dialog offers existing Accessibility settings and Use screen sharing actions; closing alone starts nothing. Existing starter still honors enabled background tracking; this does not force a backend change or grant permission automatically. Overlay/Stop behavior, identities/data, capture engine and upload locations unchanged.

Source prepared for next needed APK alongside R5 session/retry improvements. No standalone build/staging/install; current five-game APK untouched. Device acceptance pending: tap toolbar shortcut, confirm setup dialog, cancel with no capture launch, and verify desired existing setup action. R5 five-game/failure acceptance remains pending; automatic collectionOFF, no R6/merge/deploy.

Android fusionTest Kotlin compilation passed. Prior core regression checkpoint cf38933 passed165 ordinary/one private reconnect test; no core logic changed here. Repository feature/mobile-fusion-offline, same dedicated fusiontestr4 identity/signing configuration. Source publication verified separately at handoff.
