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

The scheduled R4.1 continuation began after the reset at 0% five-hour usage and 39% seven-day usage, with an 80% five-hour ceiling. No reset credit was consumed.

## R4.1 source/build result

R4.1 now reuses the tracker's existing screen-share/accessibility sources and recognition loop. It adds an explicit **Privacy first / Compatibility / No visual capture** selector with disclosure, Android 14 user-choice capture, captured-content resize handling, normalized regions, timestamp-only bookmarks, bounded next-frame manual evidence and named automatic trigger keyframes. It adds no parallel capture engine, rolling buffer or unrestricted recording. Evidence metadata calls the setting a requested policy so it does not overclaim what the Android consent dialog granted.

Ordinary core tests and `:app:assembleFusionTest` passed. The resulting 68,072,154-byte APK retains package `com.stroexd.hsdecktracker.fusiontest`, version `1.5.0-fusiontest` and label **HS Deck Tracker Fusion Test**; SHA-256 is `27e68d7e54196c192838f99d14b1067e1ee11c6ca1f52078ad04d7094b124e19`. Live usage at this checkpoint was 43% five-hour and 46% seven-day, below the authorized 80% ceiling; the reset credit remains unused.

Source-ready, tests-passed and APK-built are true. Phone-verified, merged and deployed are false. Stop before R4.2. Physical-device validation is now required for the privacy-first Hearthstone selection, compatibility disclosure/path, timestamp-only mode, manual bookmarks, landscape startup, lock/unlock, capture stop/reconnect and bounded evidence behavior.

The phone preflight identified and closed one observability/cleanup gap before asking the owner to test: the existing **Share diagnostics** action now includes R4 visual metadata and bounded keyframes without requiring continuous diagnostic recording, and **Delete** clears both diagnostics and repository-backed visual evidence. The rebuilt device package passed 126 core tests and `:app:assembleFusionTest`. Its isolated identity is unchanged; the 68,073,322-byte APK SHA-256 is `6984add76400fa8a2ea134a43bbd018cd3826a525029a6f077bbc57c4b39710c`.

The verified package/checklist is staged locally and at `H:\My Drive\HSReplay\Development\mobile-fusion-r4-1-device-validation`. Five-hour usage was 54%, below the 80% ceiling. R4.1 device acceptance remains pending the returned checklist observations and diagnostics ZIP; R4.2 remains blocked on that review rather than started speculatively.

The first R4.1 install attempt failed after the unknown-source confirmation with Android's **App not installed** message. Certificate inspection proves a signing mismatch, not a package or transfer error: the installed R3 APK certificate SHA-256 is `eb353ba7cc17f2d8aa9247b166d327a4df9527a27b3d49af0ef8f4ec9a42bfb6`; the R4.1 APK certificate is `4cf267d92ed361c32e2d4baa302670ee6c62a0cad042586e43de684ccc18f380`. Both APKs identify as `com.stroexd.hsdecktracker.fusiontest` version code `10500`, so Android correctly blocks the incompatible update. Existing phone data remains untouched. Phone readiness is withdrawn pending either an owner-approved backup/uninstall/reinstall of only Fusion Test or explicit authorization to search for the sensitive original debug signing key. R4.2 remains unstarted.

The owner authorized the signing-key search. Only the newer mismatched debug keystore exists in the user profile; no candidate exists in the HSReplay Drive tree, so an in-place R3 update is unavailable. R4 now uses the fresh side-by-side identity `com.stroexd.hsdecktracker.fusiontestr4`, label **HS Deck Tracker Fusion R4 Test**, preserving both earlier installed apps and their data. The corrected v2 APK passed assembly and independent metadata/signature inspection; it is 68,073,310 bytes with SHA-256 `a884a626b0d5abe7b4033991b6bddfc2d744a8adc07dfd78f5e581f4a1310334`. Its verified local and Drive delivery folder is `mobile-fusion-r4-1-device-validation-v2`. Android must offer **Install**, not **Update**. Usage was 68% of the five-hour window; R4.2 remains unstarted pending R4.1 phone evidence.

The owner confirmed the single-app path works after selecting Privacy First, but rejected the dropdown UX because it concealed the active risk/capability choice. R4 now shows all three modes as always-visible radio choices with full disclosures and a recommendation above them. The rebuilt APK passed assembly; SHA-256 is `4748fd2843ffcee21053be09145feb70e41b3993f7ca6e42cc5c8302a443b745`. Usage stopped at 76%, below the 80% ceiling. R4.2 remains unstarted.
