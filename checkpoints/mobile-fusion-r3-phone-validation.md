# R3 isolated phone validation

Use only the corrected R3 fusion-test APK identified in the final checkpoint. It installs as `com.stroexd.hsdecktracker.fusiontest`, appears as **HS Deck Tracker Fusion Test**, and uses separate app-private data from the working tracker installation.

The earlier `.debug` APK is superseded for this phone because Android identified it as an update to the already installed tracker. Do not install that earlier APK.

## Before installation

1. Open the working tracker and note one unmistakable existing item, such as the current deck count and newest saved match. Do not clear data or uninstall it.
2. Confirm `mobile-fusion-r3-fusiontest.apk` has SHA-256 `9744f913f7c40aaf7e4cbc1e2833a95cccb12e6eaf626f72be8df5a5ab8805d2`.
3. Install the fusion-test APK. Android must offer **Install**, not **Update**.
4. Confirm two tracker app entries exist. Open **HS Deck Tracker Fusion Test** and confirm its version ends in `-fusiontest`.

Stop immediately and report the screen if Android offers **Update** or **Replace**, if only one app entry remains, or if the fusion-test app shows the working app's existing decks or matches.

## Active-match recovery

1. In **HS Deck Tracker Fusion Test**, create or select a disposable test deck and start tracking a game.
2. Record at least one draw, opponent card, turn change and first/second-player choice.
3. From Android App info, **Force stop** only **HS Deck Tracker Fusion Test**. Reopen it.
4. Verify the same in-progress match, counts, turn and timeline return.
5. Finish it as a win or loss, then force-stop and reopen again.
6. Verify exactly one completed match exists and the finished draft does not reopen.

## Evidence import and retry

1. Open that completed match and tap the timeline-shaped evidence-import action.
2. Cancel the picker once. Verify the app remains stable and no success is claimed.
3. Try a file the picker cannot read, or temporarily deny file access if Android offers that control. Verify the match shows **Import needs attention** and **Select bundle again**.
4. Tap **Select bundle again** and choose the matching post-match exporter ZIP. If the match has unique supported evidence, verify **Fused artifact saved locally**. Otherwise verify **Needs a unique match**; that pending result is conservative, not a failure to bypass.
5. Force-stop and reopen **HS Deck Tracker Fusion Test**. Verify the displayed fusion status persists.

## Isolation closeout

1. Reopen the original working tracker.
2. Confirm its noted deck count/newest match and normal operation are unchanged.
3. Report the Android version, phone model, fusion-test app version, each result above, and any screenshot or error text.

Do not merge, deploy, uninstall the working tracker, clear its data, or continue into R4 during this validation.
