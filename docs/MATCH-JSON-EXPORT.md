# Automatic completed-match JSON export

In Settings, turn on **Auto-export completed matches**. In the Android folder picker,
create/select **Downloads/HSDeckTracker/Matches**, then allow access. Android 11+
prevents choosing the Downloads root itself; select the Matches subfolder. The
**Choose match export folder** button lets you change or renew access. Cancelling
the picker leaves the previous setting unchanged. Turning export off preserves
the folder selection and existing exports.

The app uses the Storage Access Framework on every supported version (Android 8+),
with a persisted read/write folder grant, without broad storage permissions.
Select a local public folder whose provider supports creating, deleting and
renaming documents. Access may need renewing after moving/deleting the folder,
reinstalling the app, or restoring a backup on another device.

New completed tracker matches (automatic recognition, tracker or overlay result
buttons) and results added in History are exported only after matches.json has
been successfully persisted. Existing/imported matches and later edits are not
exported. Export is off until a folder is selected. Automatic recognition also
requires the existing Auto-record matches setting.

Each file is the existing MatchRecord JSON format, using PrettyJson and the
MatchRecord serializer. It includes every record field and the entire ordered
timeline, including its existing compact t/k/d/c event keys. Omitted null fields
retain the existing serialization convention. Filenames contain the match's UTC
millisecond timestamp and a SHA-256 digest of its complete ID, e.g.
match_19700101T000000000Z_<64 hexadecimal ID digest>.json.

Writes run on the IO dispatcher, serialized by a mutex. An existing final
filename is skipped. A new document is first written as .json.part and renamed
only after the stream closes. Leftover staging files for the same match are
removed before a repeat attempt. Failed writes attempt to remove only their own
staging document. The final filename is never overwritten. The staging document
uses application/octet-stream, so providers do not append another JSON extension.

Export errors are logged and show a notice; the saved match remains in History.
There is no background retry/backfill queue. Process termination between saving
and export can leave a match unexported. If an export fails, fix/reselect the
folder; use the existing full JSON backup to recover that match's data.

To sync to Google Drive, point your Android folder-sync app at Matches and include
only *.json (exclude *.part). Export itself does not upload anything.

## Build and install

With JDK 17 and Android SDK platform 36/build-tools 35.0.0 installed:

```powershell
.\gradlew.bat :core:test :app:assembleDebug --no-daemon
```

APK: app/build/outputs/apk/debug/app-debug.apk. The debug application ID ends in
.debug, so it installs alongside the upstream app without needing the upstream
signing key. It has separate data; use Backup export/import if needed, then select
the match export folder in the debug app. Disable automatic app updates in Settings
when testing this fork, because the existing updater points at upstream releases.

A replacement for the upstream production app needs its original signing key.
Do not uninstall the original app merely to install an independently signed APK
without first preserving its data.

The fork's Android CI workflow supports manual dispatch, runs :core:test, builds
an APK and uploads the hs-deck-tracker-apk artifact. The inherited Release workflow
requires the original signing password for the inherited encrypted signing keys;
creating replacement keys would break updates. Use the debug APK for this patch.

## Device acceptance check

1. Select the public folder, enable export, finish a match, and check that exactly
   one .json appears with the same ID/result/timeline as the match in History.
2. Finish a second match and verify a second distinct file.
3. Restart the app/device and finish a match; no new picker should be needed.
4. Turn export off, finish a match, and check that History grows but the folder does
   not. Turn it on again to export subsequent matches.
5. Revoke/remove the chosen folder; a completed match should remain saved and show
   the export failure notice. Reselect a writable folder for subsequent matches.
6. Validate on Android 8/9 and Android 11+; local build/tests do not prove device
   folder permissions or synchronization to Drive.
