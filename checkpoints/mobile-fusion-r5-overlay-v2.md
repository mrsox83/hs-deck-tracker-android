# HS-FUSION — discovery/overlay combined v2, 2026-10-14

Owner resumed after credential audit. Baseline clean local/remote `2fa8116ecddbef96aa7fcc46eaaa6c9d2d9f832b`, branch feature/mobile-fusion-offline. Original tracker baseline d3bc0fb remains an ancestor, preserving four patches. START_HERE, shared batches note, own TODO/checkpoints/implementation plan read. Shared canonical native Doc connector unavailable; no backlog edit/closure inferred. Only owner-approved HS-FUSION changes; no R6/merge/deploy.

## Targeted source changes

Return-to-app click-through is owner-observed but not locally reproduced on the physical device. Source contains no NOT_TOUCHABLE or hide-overlay flag. Two concrete transition weaknesses addressed without weakening Android touch/security flags:

1. Open app previously set the Compose menu state false and launched MainActivity synchronously in the same callback. Menu removal is a subsequent composition, leaving a potential popup/activity transition race. Now a cancellable composition-owned effect waits a frame after menu dismissal before launch. Bookmark/Close actions unchanged; no delayed service/global coroutine.
2. Overlay placement previously notified WindowManager only when x/y changed, even when panel dimensions changed across rotation. Window width is now explicitly the panel/bubble width; panel-height changes also request layout and notify WindowManager. Height remains WRAP_CONTENT, avoiding an invisible full-height touch blocker. Refresh-only unchanged geometry remains a no-op. Bounds still follow the actual laid-out view.

These are source-supported transition fixes, NOT proof of the exact phone fault cause. Phone acceptance below remains open; do not repeatedly optimize without device evidence. Existing freely movable/clamped bubble and first3/explicit-next3 discovery changes are packaged together. No capture-policy, pairing, retention, credentials, schema or destination changes.

## Gates / build facts

193 ordinary tests (restored verified Gradle cache result) and two freshly executed private discovery tests pass: zero failures/errors/skips. fusionTest assembly passes. APK signature, package metadata, 16KiB ZIP alignment and native ELF alignment pass.

- Package: `com.stroexd.hsdecktracker.fusiontestr4`; label HS Deck Tracker Fusion R4 Test.
- Version remains 10500 / 1.5.0-fusiontestr4, matching the existing personal test channel.
- APK size: 68,267,590 bytes.
- Public APK SHA-256 checksum: `37f46fa0f2f9a661f6b6e13e6e34936b12cc937279f4c6350a6a32cebf2cb6fc`.
- Public signing-certificate fingerprint (not private key): `4cf267d92ed361c32e2d4baa302670ee6c62a0cad042586e43de684ccc18f380`, unchanged from installed-channel deliveries.

Initial wrapper/default-home attempts hit cache permission/TLS errors; no TLS bypass. Correct offline invocation uses repo-cached Gradle8.14.3 executable, <USER_HOME>\.gradle dependencies, protected JDK21/SDK36 and local init override. Old output directory was Windows-locked; used fresh build/r5-overlay-v2 without deleting old evidence. No cause attributed to Norton. Private fixtures stayed in <PRIVATE_EVIDENCE_ROOT>.

Start usage0% five-hour/13% weekly; after verification8%/14%. Conservative75% five-hour ceiling retained; no reset consumed. Source/tests/APK-built verified; phone-verified NO. Delivery/checksum and remote push verification are reported at handoff. Next action: finite phone gate, not R6 or more speculative hardening.

## Phone gate

Update the existing R4 Test app in place; leave the working original tracker installed. No uninstall/data clearing. If Android refuses update, report it instead of uninstalling.

1. Start tracking via your normal method, show expanded overlay in Hearthstone, choose More actions → Open app. Without moving the panel away from buttons underneath, try Minimize, expand, then X. Underlying app controls must NOT activate. Repeat return once to check repeatability; report capture method and orientation if it fails. Use a finished/no-active match so closing a screen-share service cannot interrupt a real game.
2. Drag minimized bubble to the center/interior, release: it must stay there rather than snap to an edge. Change orientation and confirm reachable; expand and check Close remains reachable. Session-lifetime position only, not a persisted setting.
3. On a saved finished game, stop tracking and separately check the confirmation. Find matching logs inspects up to three ranked sessions first. If needed, explicitly Search next3; prior sessions must not be reread. Failed/incomplete search stays pending. Confirm only a supported unique candidate, export JSON, reopen and verify status/no duplicate history. Skip Retry after success; JSON export is the next step. Local export is not Drive verification.

Report these finite checks; no five-game repeat campaign required. If click-through persists, collect focused reproduction evidence before another fix.
