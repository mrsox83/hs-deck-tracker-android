# R5 discovery-v1 phone check

Update existing HS Deck Tracker Fusion R4 Test with app-fusionTest.apk. Do not uninstall/clear data or replace original tracker. Existing fused games remain preserved and cannot be searched again. No new game required if an unfused saved game still has logs on the phone.

1. Open an unfused saved game's details, Collect logs and fuse. Stop tracking if active and check the finished-match confirmation. Start/authorize Shizuku if needed.
2. Tap Find matching logs. Search should show candidates and limits without fusing or uploading anything. No latest/closest-time guess.
3. If search is incomplete because more than6 sessions exist, read candidate summaries. Use sessions for manual review on the plausible candidate, then Find match in selected sessions. This explicitly narrows scope; unsearched sessions are not ruled out. If still pending/conflicted, do not guess: screenshot/status and export searched-session ZIP(s) for investigation. Manual selection/import fallback stays available.
4. Only when search reports ACCEPTED, tap Confirm candidate and fuse saved logs. It reuses retained evidence, no fresh Shizuku read. Check resulting status/warnings, export fused JSON to the existing Drive folder, close/reopen and confirm no duplicate history. Provider write does not prove Drive arrival; agent checks uploaded bytes separately.
5. On another unfused game, start search then Close/background. Reopen should be usable; any raw evidence already retained must remain available. Already-fused game's JSON export still works without Shizuku; no need to repeat the old full recovery campaign.

Report candidate clarity, exact incomplete/ambiguous wording, whether confirmation waits for you, and the exported file. Do not force a game reconnect or revoke permissions during gameplay. New tests performed solely for development should be identified; current campaign coaching instruction and older diagnostic exclusions remain unchanged.
