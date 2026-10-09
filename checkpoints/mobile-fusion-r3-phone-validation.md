# R3 isolated phone validation

Use only the R3 debug APK identified in the final checkpoint. It installs as `com.stroexd.hsdecktracker.debug`, beside the working `com.stroexd.hsdecktracker` installation, with separate app-private data.

## Before installation

1. Open the working tracker and note one unmistakable existing item, such as the current deck count and newest saved match. Do not clear data or uninstall it.
2. Confirm the APK SHA-256 matches the final R3 checkpoint.
3. Install the debug APK. Android should offer **Install**, not **Update**, for the working tracker.
4. Confirm two tracker app entries exist. Open the one whose version ends in `-debug`.

Stop immediately and report the screen if Android offers to update/replace the working tracker, if only one app entry remains, or if the debug app shows the working app's existing decks/matches.

## Active-match recovery

1. In the debug app, create or select a disposable test deck and start tracking a game.
2. Record at least one draw, opponent card, turn change and first/second-player choice.
3. From Android App info, **Force stop** only the debug app. Reopen it.
4. Verify the same in-progress match, counts, turn and timeline return.
5. Finish it as a win or loss, then force-stop and reopen again.
6. Verify exactly one completed match exists and the finished draft does not reopen.

## Evidence import and retry

1. Open that completed match and tap the timeline-shaped evidence-import action.
2. Cancel the picker once. Verify the app remains stable and no success is claimed.
3. Try a file the picker cannot read, or temporarily deny file access if Android offers that control. Verify the match shows **Import needs attention** and **Select bundle again**.
4. Tap **Select bundle again** and choose the matching post-match exporter ZIP. If the match has unique supported evidence, verify **Fused artifact saved locally**. Otherwise verify **Needs a unique match**; that pending result is conservative, not a failure to be bypassed.
5. Force-stop and reopen the debug app. Verify the displayed fusion status persists.

## Isolation closeout

1. Reopen the original working tracker.
2. Confirm its noted deck count/newest match and normal operation are unchanged.
3. Report the Android version, phone model, debug app version, each result above, and any screenshot/error text.

Do not merge, deploy, uninstall the working tracker, clear its data, or continue into R4 during this validation.
