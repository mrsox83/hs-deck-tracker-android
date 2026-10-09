# Mobile fusion R4 authorization checkpoint

Date: 2026-10-09

Repository: `mrsox83/hs-deck-tracker-android`

Branch: `feature/mobile-fusion-offline`

Starting remote-verified tip: `250f0b6cf6e0e2f07059063a30148e6250919691`

## Authorization and boundary

The user explicitly authorized R4 after approving a staged targeted-region plan. R4 remains on the dedicated `com.stroexd.hsdecktracker.fusiontest` identity. Do not merge, deploy, alter the working tracker installation/data, begin R5, or broaden this into unrestricted screen recording.

R4 must audit and extend the tracker's existing screen-share/OCR module and frame-source lifecycle rather than build a parallel capture stack. Present an explicit privacy-versus-capability choice: prefer Hearthstone-only single-app capture when supported, while allowing an informed opt-in to the broader existing screen-share path with a warning that notifications or other non-Hearthstone pixels may be processed. Visual capture remains opt-in and timestamp-only bookmarks must continue to work without it. Do not persist an unrestricted recording or unnecessary full-display frames, and do not implement a rolling pre-event buffer; retain only bounded trigger/manual-bookmark evidence needed by the selected mode.

## Staged implementation

1. R4.1: audit/reuse the existing screen-share/OCR pipeline; add the privacy/capability selector and disclosure; then implement normalized/configurable regions, bounded keyframes, cleanup and timestamp-only fallback without a second capture engine.
2. R4.2: shadow-mode pilot groups for hero state, turn/resources and event recovery.
3. R4.3: evidence-backed per-field promotion from experimental to corroborating to accepted. Unknown and inferred values remain explicit; experimental output cannot create an authoritative event.
4. R4.4: close after bounded pilot acceptance. Defer complex board/minion, secret, deck-count and choice-panel recognition unless separately justified by real evidence.

Landscape gameplay is expected on this phone. Keep coordinates dimension/inset aware and validate landscape startup, lock/unlock, capture stop/reconnect and applicable size/inset changes. Exact battery percentage is not an acceptance threshold; the user will monitor practical demand. Obvious thermal escalation, gameplay stutter and runaway capture/storage remain failures.

After R4 source and device behavior stabilize, remind the owner that an optional Codex Security review may be useful before any public-release, merge/deployment or broader-distribution planning. The review is not authorized yet and must not block ordinary R4 implementation.

## Current state and restart point

R3 is phone accepted. Source-ready, tests-passed, APK-built, phone-verified and pushed are recorded separately; it is not merged or deployed. The tracker patches remain preserved.

At authorization, live Codex usage was 79% of the five-hour window and 36% of the seven-day window. This exceeded the owner's prior 60% five-hour ceiling, so no R4 implementation began. Resume at R4.1 after rereading live usage, `START_HERE.md`, the shared backlog and the active project plan/checkpoints. Read and verify local/remote state again before editing.
