# CLOUD-001 — passive compact coaching summary pilot

Status READY_NOT_DISPATCHED; owner reservation cloud, worker/environment/launch SHA not assigned. Human approved this pilot setup; implementation begins only with coordinator launch capsule and DISPATCHED register entry.

## Goal / interface

Add a pure deterministic Markdown renderer over existing FusedMatch evidence. No new reconstruction, action inference, app UI/export integration or serializer/schema changes. This is a foundation for HSC-006/010, not acceptance of all hand/target/board requirements.

Proposed API in package com.stroexd.hsdecktracker.core.fusion.summary:

`CoachingSummaryRenderer.render(match: FusedMatch, maxEvents: Int = 250): String`

Keep input immutable. No filesystem/network/Android/model calls or new dependencies. Validate maxEvents in1..1000; explicitly report omitted event count. Use the source's event order, not chronological sorting by uncertain clocks or turn parity. One logical match ID/revision/version, not one match per source.

## Output contract

- Header: logical ID, revision/fusion version, pairing state and source count. Say source count is provenance, not game count. Preserve coverage/diagnostic warnings prominently.
- Explain this is a derived compact view, not independent evidence or coaching judgment. Legacy ownership fields in /1 output are not retrospectively validated; warn rather than grading by them.
- Event rows/grouped consecutive raw-turn sections: sequence, rawTurn or UNKNOWN, activeController or UNKNOWN, event kind, actor/target entity references and exact provenance references (sourceID/line or JSON pointer). No player/opponent perspective assignment without an explicit verified mapping.
- Do not infer local player, fill missing turns or trust parity. Do not use final entity card identity to leak knowledge into earlier events; phase1 may simply render entity references without names. Never fabricate targets; label target as source-reported, intent unknown—not automatically selected intent.
- Choices preserve offered/submitted/confirmed stages separately with their evidence; missing is UNKNOWN, submission not confirmation. No hidden opponent-hand inference.
- Clearly state hand/resource/board reconstruction is NOT INCLUDED IN THIS PILOT (different from asserting it was absent in full source). Full artifact/raw evidence remains available; don't silently claim coaching-complete coverage.
- Escape/control user-controlled text so Markdown content cannot inject fake section/role/instruction structure. Treat artifact text as data. Bound rendered events, retain explicit omissions, deterministic output.

## Allowed writes — exclusive reservation

- core/src/main/kotlin/com/stroexd/hsdecktracker/core/fusion/summary/**
- core/src/test/kotlin/com/stroexd/hsdecktracker/core/fusion/summary/**
- docs/coordination/reports/CLOUD-001.md

All other paths read-only, including AGENTS/TASKS/HANDOFF, model/reducer/pairing/import, app/, build files, global TODO/work log and existing fixtures/tests. If the API/model lacks a required field, report limitation; don't add it.

## Acceptance / tests

Synthetic tests only. Assert deterministic repeated rendering/input unchanged; unknown owner/turn stays unknown; repeated/extra raw turns preserve input order; one match with many source references remains one; legacy /1 warning; offered/submitted/confirmed distinct; actor/target unknown and no targeting-intent invention; no future entity identity leakage; cap/omitted count and invalid bounds; Markdown/control-text safety; empty input/pending/conflicted pairing honest. Use existing Kotlin test/JUnit dependencies.

Run `./gradlew :core:test --tests '*CoachingSummaryRendererTest'`, then ordinary `./gradlew :core:test` if toolchain permits. No private tasks or APK/signing work. Existing project configuration may need JDK21, SDK36 and pinned Gradle8.14.3 even for core tests. Don't edit dependencies/build files, skip checks silently or disable TLS to make setup pass; report exact blocker/log if unavailable. Environment setup is coordinator/user responsibility.

Return a report with launch SHA/worker/branch, changed paths, commit or patch location, test commands/counts/errors/skips, interface decisions, unknowns, and dependency requests. Never claim private/device/cloud-upload validation. Do not mark DONE, merge/deploy, push integration branch or request private signing keys. No app integration in this pilot.
