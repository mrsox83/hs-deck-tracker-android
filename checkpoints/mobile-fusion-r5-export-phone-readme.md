# R5 manual-v3 — corrected fusion evidence export

Supersedes manual-v2 for export validation: screenshot9344 exposed a filename guard bug; fused logical IDs differ from tracker record IDs. v3 corrects that guard with canonical path protection intact. Status now stays in a contrasting panel above scrolling controls. Do not re-fuse/delete data merely to work around the v2 error; update then export the already-saved game. Phone/provider success remains pending.

Update only HS Deck Tracker Fusion R4 Test; do not uninstall or clear app data. Working tracker remains separate. Keep the companion exporter fallback. All validation games/exports are TESTS excluded from coaching; these instructions do not mutate the coaching ledger.

1. Install the new manual-v2 APK as an update. Verify your existing R4 Test history remains present.
2. Open an already-fused game's details and its Collect logs and fuse dialog. Stop tracking is now directly above the confirmation checkbox. It stops tracker/overlay, but does NOT check the checkbox or start collection.
3. Tap Export retained logs bundle. No Shizuku connection is needed for retained evidence. Android's save picker opens; choose your existing HSReplay Drive folder. Keep the default fusion-test filename. The ZIP contains original retained Power.log evidence and rebuilt companion-contract match documents/manifest; it may include multiple games from the session. It does not claim a unique pairing.
4. Wait for “Fusion evidence written to selected destination” and the in-dialog byte count. Provider-write success is not independently verified cloud sync. If cancelled/failed, the dialog reports it and private originals remain; a failed destination may be partial. Do not treat it as ready evidence.
5. Tap Export fused artifact (JSON) for the same game, select the same Drive folder and await confirmation. This requires a saved local fused artifact; pending/no artifact produces an explicit error. The JSON and raw ZIP are separate files with separate purposes.
6. Report the filenames/outcomes. HS-FUSION will inspect actual Drive arrivals, hashes, provenance and warnings before acceptance. No need to re-play the game. Try cancelling one picker and confirm it reports cancellation, not success.

Opening the destination picker closes the restricted reader safely but keeps this dialog for its export result. Close/reopen to connect it again; retained retry/export remain available without Shizuku. Process death or closing during export is not resumable cloud upload; private originals permit retry. Never uninstall/clear data to find the internal path.

Also included: readable session dates and selected-game context; explicit already-fused preservation feedback; Tracker OpenInNew shortcut opens the same setup dialog as Settings. Automatic candidate discovery remains queued, not included. Automatic collectionOFF; no R6/native Drive API, merge or deployment. Device/provider behavior remains pending owner validation.
