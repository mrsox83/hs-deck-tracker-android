# R5 manual pilot — phone validation

R5 manual-v1 APK built and identity verified. Delivery directory: H:\My Drive\HSReplay\Development\mobile-fusion-r5-manual-device-validation-v1. APK SHA256 a783cadeb7893c39a6a9141e9993fcc38d6b0d79ca04f1de07e295d1e1814f6d, 68,189,554 bytes. Signer SHA256 4cf267d92ed361c32e2d4baa302670ee6c62a0cad042586e43de684ccc18f380, unchanged from accepted R4 Test. Package remains com.stroexd.hsdecktracker.fusiontestr4, versionCode10500/versionName1.5.0-fusiontestr4; label remains HS Deck Tracker Fusion R4 Test to preserve its existing installation/data. This is a personal isolated test update, not a release of the working tracker. Phone acceptance remains pending.

## First gate (one match, not five yet)

1. Install the delivered R5 manual-v1 APK as an update to **HS Deck Tracker Fusion R4 Test**. Do not uninstall any tracker. Confirm old working tracker and R4 Test history/decks are still intact. If installation fails, report the exact message; do not delete app data.
2. Start Shizuku as usual. Its authorization is separate for this tracker identity, even if the companion exporter already has access. Keep the standalone exporter installed as fallback.
3. Play/finish one convenient test match with the Fusion R4 Test tracker. Let it save the completed game, then stop active tracking. Do not restart Hearthstone during an active match. All games/artifacts made for this validation are TESTS, excluded from coaching.
4. Open that saved game's details. Tap **Collect logs and fuse — manual pilot** below the game header. Read the disclosure and check **I have finished the match and stopped active tracking**. This reader does not need screen capture or Accessibility, and never changes Hearthstone's originals.
5. If prompted, tap **Authorize restricted reader** and grant Shizuku permission to this tracker. If opening another app or the permission flow closes the dialog, reopen the same game's manual dialog and reconfirm the finished-match checkbox. No collection starts just by granting permission.
6. Tap **Connect / refresh sessions**. Select the session whose date/time contains your match; nothing is selected automatically. Select two only for a known reconnect split. Tap **Collect selected logs and fuse** and keep this screen open.
7. Report the exact visible outcome. **Fused locally** means local fusion only, not upload or gap-free capture. Pending pairing is an honest non-success: share its reason, selected session times, and a companion export through the existing destination if requested. Read failures/raw-only status do not mean a missing competitive game. Raw logs may contain account identifiers.
8. Close/reopen game details. Confirm the fusion status survives and history still has one record for this game. Reopen the manual dialog and use **Retry fusion from retained logs (no Shizuku read)** once. An already-fused match must stay preserved, with no second history record. Existing fused matches are deliberately not overwritten by fresh collection in this pilot.

No new Drive folder permission is required for this flow: raw logs and generated bundle stay private in the tracker. The feature does not upload them. Existing match export/companion export destinations remain unchanged. Screenshots of the status are sufficient for the first report; do not upload unrelated games or sensitive account data unnecessarily.

## After the first gate passes

HS-FUSION will request the remaining bounded checks: permission denial/no-success, close/background interruption and retained-log retry, Shizuku stopped/restarted, unreadable/empty/truncated session warnings, and eventually five completed matches without duplicates. No need to force-stop repeatedly or begin five games before the first integrated read/fusion is confirmed. Automatic collection remains off until manual acceptance. R6/native Drive integration and continuous tailing are not included.

Technical limits: one or two selected sessions; 16 MiB raw/session; 16 MiB decoded document and 32 MiB decoded documents/session. Source writer truncation remains possible; exporter cannot restore omitted data. Completion markers do not certify gap-free evidence. Backgrounding closes the manual reader; it does not change the separate capture engine. Synchronous Binder waits cannot always be interrupted immediately.
