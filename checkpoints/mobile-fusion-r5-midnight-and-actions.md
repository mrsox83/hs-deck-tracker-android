# R5 midnight pairing fix and action clarity — 2026-10-12

Owner requested clearer non-link buttons for the next build and resumed diagnosis of screenshot9362 pending pairing. Scope HS-FUSION R5 manual acceptance, existing HSC-009 journal / HSC-013 coaching-reconciliation context. No automatic discovery/collection, R6, merge or deployment.

## Reproduced operational defect

Tracker4ae366a8-7638-4727-804e-68a4326c8a48, Death Knight/Mage LOSS, timestamp2026-10-12T05:14:31.002Z. Uploaded bundle session Hearthstone_2026_10_12_00_08_50 contains two completed matches ending00:14:24.1336240 and00:28:25.1832390; raw source hash43cc8b2c630c8c016123c429d1aba73a6959fd5d09b0b067de79ee67b1ad3fe0 matches manifest. Files are saved correctly, not fused on the old app.

Exact ZIP SHA256bec5952bf14a6d2e430b356ef7def161dec642eaa62e3b43ce87a4164419d32e; tracker file SHA256cb7a5a454066d84a92c1a37cd1c05de7174b88892dc0d1dc46c548c89c208829. RealPendingPairingTest requires these private inputs explicitly; none are in Git.

PowerClock used sessionStart.minusHours(12), which wraps LocalTime. For00:08 sessions the threshold became12:08, incorrectly placing00:14 on tomorrow's date. This invalidated the two-minute endpoint gate. Fix uses a non-wrapping seconds difference with the existing greater-than12-hour backwards threshold. No tolerance or identity gate was relaxed; true late-night rollover behavior is preserved.

Before fix the exact two-game bundle is pending due endpoint disagreement. After fix the first game pairs uniquely with matching result and a6-second integer delta (actual approximately6.87seconds). The later match stays rejected and retained; artifact warns selected other-game evidence is not incorporated. This warning is not proof of missing same-game coverage. No automatic rewriting of accepted artifacts or creation of tracker history records.

## Actions and recovery

Full-width filled/outlined action buttons replace retained-export/retry/fused-export link text. Labels: Export saved logs (ZIP), Retry using saved logs, Export fused game (JSON). Explanations distinguish evidence export, recovery, new reads and cloud arrival. Retry disabled after success; JSON enabled only when a FUSED_LOCAL journal entry has an artifact filename. Repository still validates actual file existence/path on export and reports missing files, never claims success from an enabled button. Saved pending/success state and coverage warnings initialize on dialog reopening. Stop/collect actions are also full-width buttons; stop does not check confirmation.

Current campaign coaching eligibility is owner-authorized, not overridden by fusion-test filenames. Blanket TESTS UI wording replaced with owner-instruction/diagnostic distinction. Older exclusions remain. No coaching ledger changes or analysis performed.

## Validation and release boundary

172 ordinary tests, one exact pending-game private regression and six full private fusion corpus tests pass with zero failures/errors/skips. Ordinary cases include early-morning/same-day/actual-midnight rollover/small backward time/invalid dates. Android assembly passes; final UI packaging and APK identity/hash/delivery verified separately before handoff.

Initial test method returned a non-Unit assertion value; corrected. Windows locked generated test binary output was preserved and avoided with fresh output roots. Approximate7s expectation corrected to actual integer6s. These initial validation failures are not remaining source defects. Cached offline toolchain used, no TLS/security bypass.

Usage at start12% five-hour/7% weekly, after gates18%/8%, ceiling90%, no reset consumed. Branch start18a1e2341ca9dbeba1a3fe5e4b03045662b50903 remote verified; four tracker ancestors preserved. Original fixtures/installations preserved. Manual-v4 phone verification pending; instructions in mobile-fusion-r5-v4-phone-readme.md. Recover the existing game with retained retry, then exportJSON, verify status/no duplicates; no new game requested. Broader five-game and failure/recovery acceptance remain open.

Final APK:68,486,592bytes; SHA256ee657242d3ebf25c286478447c26187c4657c85dec885e993ff4792141eaf14c. Packagecom.stroexd.hsdecktracker.fusiontestr4, version10500/1.5.0-fusiontestr4, signerSHA2564cf267d92ed361c32e2d4baa302670ee6c62a0cad042586e43de684ccc18f380 unchanged; signature and16KiB ZIP alignment checks pass. Final assembly rerun includes saved pending-state initialization. Delivery Development/mobile-fusion-r5-manual-device-validation-v4, with README/checkpoint/checksum; actual copied hash and remote branch tip checked at handoff. Source/tests/APK-built verified, phone acceptance pending, no merge/deployment.
