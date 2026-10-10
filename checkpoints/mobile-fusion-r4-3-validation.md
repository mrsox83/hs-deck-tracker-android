# R4.3 per-frame validation checkpoint

Owner authorized the bounded transition on 2026-10-10, with a 90% active five-hour ceiling. Start usage 30% five-hour / 65% weekly; remote/local tip 32c98e2 and all four tracker ancestors verified. Active phone identity remains com.stroexd.hsdecktracker.fusiontestr4. No R5, merge or deployment.

Implemented a pure, additive assessment API, separate from the diagnostic candidate and all authoritative tracker/fusion events. Exact frame SHA256 and actual frame capture time (not bookmark tap time) bind each field review. References include review kind, source SHA256, source pointer, reviewer and optional visual parent lineage. Callers must verify referenced source content and temporal alignment before supplying a non-visual review; this API is not a source loader or pairing engine.

Finite policy:

- Unknown, reused, unsupported rule/value and missing OCR support cannot promote.
- Only exact turn affordance, friendly/opponent numeric health and mana pairs are eligible. Layout cues and untyped adjacent counters remain inferred/experimental.
- One matching human image annotation or independent source is corroborating, medium confidence. This is not a calibrated probability or a global reader accuracy claim.
- Accepted requires agreeing human image review plus a distinct independently referenced non-visual source. Same-artifact aliases, repeated reviews and visual descendants cannot manufacture independence.
- Any valid disagreeing review yields conflicted/null while preserving the original candidate and every alternative review. Incoming candidate stage is not trusted.

Tests-passed: 141 ordinary tests and three required private replay tests, zero failures/errors. Synthetic cases exercise accepted, corroborating, conflicted, unknown, duplicate/lineage/identity rejection and serialization round-trip. Diagnostic (11) real replay yields corroborating mana only for 1/2, 10/10 and 3/10; missing/reused/menu frames remain experimental/unknown. Real accepted observations: NONE. No independent non-visual alignment has been verified for those frames. Current live capture still emits experimental candidates; the core assessment API does not silently promote them or mutate stored match records.

Bounded next gate: evaluate a source-bound assessment only when independently aligned non-visual evidence is already available, or explicitly close this release as corroborating-only enrichment. Do not invent a new capture campaign, additional readers or R5 adapter to force acceptance. The existing source gaps are valid unknowns. R4.4 closeout must document the chosen limited acceptance scope; surface R5 separately.

APK/publish details are recorded in WORK_LOG. Existing v5 phone capture is verified; fields-3/assessment execution on a newly delivered APK is not yet phone-verified. No repeat lifecycle/toolbar/four-card phone script is needed.
