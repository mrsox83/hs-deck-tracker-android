# R5 game 1 acceptance and coaching eligibility — 2026-10-11

Origin: HS-FUSION. Target: HS-FUSION acceptance / HS-COACHING ingestion.
Existing context: HSC-009 journal/deduplication; HSC-013 coaching reconciliation. No canonical DONE claim.

Owner confirms PASS: status survives detail reopen; already-fused retry produces no duplicate history; status panel remains visible/readable; Stop tracking does not automatically check confirmation.

Game 1 tracker ID: 861b778a-104c-4d6d-b741-07acea8ba6a7.
Logical fused ID: e14389a4d8e0258210404db2203d1bd7.
Source session: Hearthstone_2026_10_11_15_15_54.
Prior verified Drive evidence:
- fusion-test-861b778a-104c-4d6d-b741-07acea8ba6a7-3c64a536-ae21-41b8-af04-505f88472d08.zip; SHA256 60e116086d4cbe063709a9228f06dcb0fb6df755b7b691007b87c6701a9b0d5d.
- fusion-test-861b778a-104c-4d6d-b741-07acea8ba6a7-e14389a4d8e0258210404db2203d1bd7.json; SHA256 a7b8df2614b457bc43b55dc38c58311ad397ce81b31ea5cc515b3bfab1f56f09.

Owner explicitly authorizes this game and the next four games in the normal-use campaign for coaching. This overrides earlier campaign-wide exclusion and fusion-test filename inference. Do not reclassify older diagnostic/test games. The next four exact IDs must be reconciled when reports arrive. Treat raw ZIP, decoded match, tracker JSON and fused JSON as one lineage, not separate games. Coaching ledger and analysis are owned by HS-COACHING and are not updated by this note.

Build: manual-v3, implementation 571622e, package com.stroexd.hsdecktracker.fusiontestr4. Source/tests/APK were verified in its release checkpoint; above phone checks now user-confirmed. Games 2-5, permission/Binder failure and interrupted raw-only recovery remain open. No new build, automatic collection, R6, merge or deployment.
