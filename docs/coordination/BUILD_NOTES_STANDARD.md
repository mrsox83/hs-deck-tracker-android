# Build notes and Resource Exchange

Owner-approved 2026-10-19. Use the full Resource Exchange name in replies and documentation; recognize the owner's Re/RE shorthand. Its existing physical folder remains `H:\My Drive\HSReplay\Development\00 - Shared Exchange`. Do not rename or relocate it.

## Authority and discovery

The consolidated cross-project release reference is [HS-FUSION-PATCH-NOTES.md](https://drive.google.com/file/d/1awcL4HLD7xuCX76hQt69MIoWaPKx7j7r/view?usp=drivesdk) in Resource Exchange. Resource Exchange `START_HERE.md` also carries this verified Drive link. This repository file is the publishing standard, not another changelog. Keep source/private evidence and project plans in their existing owned locations. Patch notes do not override the canonical HSC backlog or owner approval gates.

Before reporting or triaging an existing symptom, read the shared patch notes and identify the affected APK by exact folder/hash, not the reused Android display label/version. Keep capture time, fusion producer and currently installed app distinct. An older immutable fusion does not gain fixes when the app updates. Missing identity is BUILD UNKNOWN, not proof of a regression.

Report older evidence under its existing HSC ID as HISTORICAL / ADDITIONAL EVIDENCE where applicable. Source-only work remains VALIDATION PENDING. A recurrence on the exact corrected build is POSSIBLE REGRESSION / STILL REPRODUCIBLE and must not be suppressed. Preserve raw evidence; reporters leave scheduling priority UNASSESSED under BACKLOG_PRIORITY_STANDARD.md. Receipt, implementation and acceptance remain separate.

## Every new APK package

For JSON source comparisons during validation, follow JSON_EVIDENCE_COMPARISON_STANDARD.md. Keep exact original byte hashes distinct from parsed-content equality and serializer-hash reconciliation; a pretty/compact presentation difference is not automatically corruption and never licenses rewriting source evidence.

1. Add a dated entry to the same shared patch-notes file, preserving its Drive ID/link and old entries. Include exact package folder/link, source SHA and dirty-build qualification, APK checksum, HSC/task IDs, fixed/new behavior and limitations.
2. Record compatibility/schema changes and migration/user steps, including an explicit none when verified. Separate source tests, APK/static/signature gates, delivery, phone validation, push, merge and deployment. Phone starts PENDING until observed evidence arrives.
3. Retain the package's own README.md, CHECKPOINT.md and SHA256.txt inside its unique sibling folder directly under Development. Link from the index; never nest builds or overwrite a historical package to update its status.
4. Verify actual mounted content and connector readback of the shared index. Check the Resource Exchange startup pointer resolves. If sync/access is pending, state that publication is pending rather than claiming other agents can read it remotely.
5. On acceptance, append/update a dated bounded acceptance result with evidence. Do not close broader HSC issues on build delivery alone. On source-only work, update the unreleased section and later name its FIRST DELIVERED build/checksum; a commit is not that delivery.

The initial index covers all26 existing package folders but does not invent missing historical dates/checksums/acceptance. Follow older package notes and newer explicit owned acceptance for those cases. No other project's logs or coaching ledger are edited by this standard. Resource Exchange is read on startup; no automatic messaging/monitoring is created.
