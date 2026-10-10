# R5 post-match collection — authorized foundation

Owner authorized the reviewed R5 proposal on 2026-10-10 with a 90% active five-hour ceiling. This supersedes the proposal's earlier not-authorized status, not R6 or continuous-tailing gates. Start usage 50% five-hour / 68% weekly; latest sample 56%/69%. No reset consumed.

## Source inventory and ownership

Repository mrsox83/hs-deck-tracker-android, feature/mobile-fusion-offline, clean remote-verified starting tip d3521197819faad35c60207707167ac82d7bd301. Keep com.stroexd.hsdecktracker.fusiontestr4 identity and signer; preserve working tracker/R3/R4 data and four tracker patch ancestors. No app code or identity change in this foundation.

Pinned source: H:\My Drive\HSReplay\HS_Post_Match_Exporter_Source_v0.4_2026-10-07.zip, 534157 bytes, SHA256 de1cee05e9df28a98ab4a0755d8d5a56995c11fb943ad922e3c8e9358c05f153. This matches R0, but does not establish the phone's installed companion version. Original archive remains untouched.

Inspected ILogReader.aidl, LogReader.java and acquisition/binding portions of MainActivity.java. Exact v0.4 Shizuku API/provider dependencies are 13.1.5. Reader permits only sorted Hearthstone_[0-9_]+ directories under /storage/emulated/0/Android/data/com.blizzard.wtcg.hearthstone/files/Logs and read-only Power.log; canonical-path check rejects escape. AIDL exposes listSessions, openLog and destroy, no generic shell or write API. Non-daemon user service and explicit permission/binder-death/unbind lifecycle are required; do not tie collection to capture/Accessibility foreground service.

Source export uses a fixed opened-descriptor size (16 MiB/session limit), preserves raw Power.log, emits hs-export-bundle/0.4 manifest and hs-power-evidence/0.3 match documents. Existing tracker EvidenceAdapters/FusionImporter already consume that bundle contract and conservatively pair rather than guessing latest match. Full parser review/reuse and Android null-descriptor handling remain assigned, not claimed implemented.

HS-FUSION owns optional tracker integration; no HS-EXPORTER source/UI or coaching plans edited. Link HSC-004/HSC-009 for existing fusion/journal context; no canonical DONE claim or newly copied other-project plan. Root synced tree, existing destination and private-fixture policy remain unchanged.

## Implemented boundary

PostMatchLogSnapshot is a pure JVM primitive, with injected stream/observed descriptor size. It validates session names and bounds before opening, copies at most 64 KiB/read, excludes subsequent appends, rejects premature EOF/stalled reads, checks cancellation and closes streams. SHA256 identifies actual copied bytes, independent of session aliases; copied bytes are defensively exposed.

Limits: not a complete-log detector, stable-file guarantee, durable collection journal or logical-match deduper. Equal-length concurrent rewrites are not detected. Rotation safety depends on caller binding size and stream to the same descriptor; caller must close the descriptor even if validation fails. Parser must independently report incomplete capture. No new permission/capture engine/automatic service/storage destination.

Nine synthetic regression cases cover exact hash/bytes, invalid paths, size bounds, append exclusion, short read and close, missing/unreadable, cancellation before/mid-read, stalled read and repeated/rotated content identities. Ordinary core suite passed: 153 tests, zero failures, errors or skips. Private fusion/visual fixtures not rerun because adapters/reducer/field logic are unchanged. No APK build or phone acceptance claimed.

## Next finite unit

HS-FUSION: wire restricted reader and Shizuku permission/binder/death lifecycle, review parser completely before reuse, retain raw snapshots in private recoverable journal, and build foreground user-triggered Collect logs and fuse using existing importer/outbox. Honest denied/missing/raw-only/pending/local statuses; retry must not duplicate logical matches. Keep companion fallback and existing Drive/folder route. Do not turn on automatic post-match collection until manual phone validation is proven. Alert owner when isolated APK is actually ready.

Later acceptance: five completed matches without duplicates, interruptions/retries and missing/truncated/rotated/repeated sources. Capture opt-in is independent; timestamp bookmarks remain available. Saved/local, tests, APK-built, phone-verified, pushed, merged and deployed remain separate facts. R6/native Drive and continuous tailing require separate authorization.
