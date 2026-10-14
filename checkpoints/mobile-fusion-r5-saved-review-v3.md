# R5 saved candidate review v3 — 2026-10-14

Owner approved fixing recovery after screenshots9446/9448/9450/9456. Branch feature/mobile-fusion-offline, clean remote d662d70 verified before edits; original tracker patches preserved. Own TODO/current v2 feedback and shared START_HERE read; no new shared notes in inventory, native canonical backlog connector unavailable. HSC-009 context, HS-FUSION-owned implementation only. No standalone exporter/coach plan changes, R6, merge/deploy, reset or source relocation.

## Observed acceptance / evidence

Overlay issues owner-confirmed resolved in v2; do not reopen the phone loop. Oct14 latest uploaded raw bundle has verified raw SHA25675498254000df9a0ca6755ddcdb993bf8432fc925cd89c2213556d3c63820f7a, session15:50:04, completed match. Two ZIP wrappers contained the same raw source, not extra games. Later successful fusion arrived: hs-fused-match/1 ACCEPTED,414events/562snapshots/0diagnostics. These are technical checks; coaching eligibility/ledger not changed.

## Fix and boundary

Broad discovery still stays incomplete on read/parse failure, and next3 still carries failures forward. Do not waive that guard or call null descriptors empty/irrelevant. Existing button only selected checkboxes and required the owner to rerun search; this caused the operational confusion.

**Review this game's saved logs** now explicitly narrows to the candidate's one/two saved session aliases and immediately re-evaluates them with no Shizuku call. It verifies match ownership and saved bundle hashes, rejects parser/hash failures/missing scope, reduces EVERY game in those sessions, enforces decoded-match/byte/cooperative-time budgets and applies the unchanged strict evaluator. Choosing a row never means selecting just that source match out of a multi-game session. Ambiguity remains conflicted. No new collection entry, raw read, fusion or history record is created by review.

UI disclosure says other unreadable/unsearched sessions are excluded, not ruled out. Earlier warning/context retained in full status; they are not misrepresented as failures inside the newly chosen readable scope. Separate Confirm candidate and fuse saved logs revalidates retained inputs through the existing importer. Back to all search candidates restores the original incomplete preview and allows another candidate/next batch without losing saved evidence. Review does not require a live reader connection, but does require finished/stopped confirmation and an unfused saved game.

Existing capture, package identity, signer, artifact schema, sync paths and pairing rules unchanged. No automatic fusion, global uniqueness claim, arbitrary reader API or native Drive upload added. Closing discards transient preview as before; retained files remain.

## Tests / handoff

Synthetic regressions cover unreadable sibling -> broad PENDING -> explicit saved review ACCEPTED, no new reads/collection/history, warning retention, wrong ownership and modified file rejection; two plausible games in one selected session remain CONFLICTED. Exact private Oct14 fixture covers the uploaded manual record/log plus simulated null descriptor sibling, expected414events/562snapshots/provenance and repeat-import identity. Older private discovery/reconnect cases retained. Initial new JUnit test returned an exception object instead of Unit; corrected before final gates, no production workaround.

Start live usage14% five-hour/23% weekly; conservative75% five-hour ceiling, no reset used. Final gate/build/delivery details appended below. Windows lock on old build output was avoided using fresh build/r5-recovery-v3; no evidence deletion or TLS/security bypass. Phone acceptance remains pending even when build passes.

## Final verification

195 ordinary and3 private discovery tests pass, zero failures/errors/skips. Exact latest upload reproduces414events/562snapshots, valid provenance and repeat-import identity. Assembly/signature/package/ZIP16KiB/native alignment pass. No unrelated full lifecycle corpus rerun. APK68,277,634bytes; public APK SHA256 checksum `dbc7bb016d44f59a02d8eb0bece7eeebefa21c1abf301a73df8cc8117f306314`. Same package com.stroexd.hsdecktracker.fusiontestr4/version10500/1.5.0-fusiontestr4. Unchanged public certificate fingerprint (not private key) `4cf267d92ed361c32e2d4baa302670ee6c62a0cad042586e43de684ccc18f380`. Delivery Development/mobile-fusion-r5-discovery-device-validation-v3; old deliveries retained. End usage21%five-hour/24%weekly, no reset. Phone acceptance pending. Delivery/readback/push results in final handoff.

## Finite phone check

Delivery completed: Drive APK reread with matching checksum; own checkpoint copied to the owning development folder. Scoped cleanup removed0.6GiB disposable build intermediates/tmp, preserving APKs/reports/fixtures/cache. Git publication verified separately at handoff.

Update R4 Test in place, no uninstall/data clear. On the next unfused saved game: stop tracking, separately confirm finished/stopped, Find matching logs. If old unreadable sessions block it, click **Review this game's saved logs** below the matching candidate. No checkbox selection or second search is needed. Expect a supported recommendation only within the disclosed saved scope, then **Confirm candidate and fuse saved logs**, then **Export fused game JSON**. Skip Retry after success. Reopen status/no duplicate history; upload arrival verified separately. If uncertain, Back to all search candidates; do not force pairing. No overlay/five-game campaign required.
