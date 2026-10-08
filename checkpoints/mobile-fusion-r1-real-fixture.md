# Checkpoint: first R1 real-fixture gate

Date: 2026-10-08. Scope remains offline R0–R2 only.

## Gates

Ordinary core tests, which do not access private fixtures:

```text
gradle :core:test --offline --no-daemon
BUILD SUCCESSFUL
```

Explicit private fixture gate:

```text
gradle :core:realFusionFixtureTest -PhsFusionFixtureDir=<private directory> --offline --no-daemon
BUILD SUCCESSFUL
```

The private test class is excluded from `:core:test`. The dedicated task fails before executing tests when the fixture property is absent; it cannot silently return a green result without fixtures.

## Verified representative pair

- Tracker SHA-256: `d9b037917de5007d456b305850d3dd9e6ddaf919d59b8e2928d08de31057d574`.
- Export bundle SHA-256: `6e89dacbe05f040a10ab5a73ac79e2edc31934d2c3e3fbf18d94a981ff038d4e`.
- Inner Power.log SHA-256: `2c05f4bf27aca34e440ef4fc3a554759ffe40605f01beaa9bdf3193b52b38744`, equal to the manifest declaration.
- Bundle schema `hs-export-bundle/0.4`; one completed, non-truncated match; 22,601 selected-stream records; nine choices.
- Tracker WIN, 18 reported turns and 73 timeline records.
- Choice 8 retains Raze (`CATA_190t12`) independently at submitted and confirmed stages.
- Choice 9 retains Enthrall (`CATA_190t13`) independently at submitted and confirmed stages.
- Master Dusk (`TLC_513t`) PLAY at source line 38189 precedes Deathwing (`CATA_190h`) PLAY at source line 45191.
- Target `0` on both PLAY blocks is represented as no explicit target, not an entity or enemy face.
- Session-local end time resolves in America/Chicago to seven seconds before the tracker timestamp.
- Pairing is accepted only after endpoint, result, first-player and anchor evidence agree; replaying the same reduced source does not duplicate it.

## Verified truncated multi-match bundle

- Bundle SHA-256: `9c731e4ce8c09a710d971cb0fa5b13dccee8cf12dc7c1fc6bfaec5e4e14fd71f`.
- Legacy schema `hs-export-bundle/0.3`, whose manifest does not expose the newer truncation field.
- Inner Power.log manifest SHA-256 matched the raw entry.
- The raw Hearthstone size-limit marker was independently detected and its source line retained.
- Three source matches were reduced: completion states `[true, true, false]` and truncation states `[false, false, true]`.
- Earlier completed matches were not contaminated by a later session truncation.
- The incomplete suffix retained its observed events and a last-valid snapshot instead of being discarded.

## Verified duplicate-wrapper identity

- `HS-export-20261007-110747-2272cd33.zip` and `HS-export-20261007-110749-cf440038.zip` have different outer SHA-256 identities.
- Their inner Power.log hashes and per-match source identities are equal.
- The fusion coordinator retains both outer artifact hashes as aliases on one Power source.
- Replayed wrappers do not create duplicate canonical events or independent corroboration.

## Boundary

The scoped R1 checklist is complete: schema/adapters, identities, conservative pairing, replay deduplication, compact provenance, shuffled/repeated import, composite and ambiguous rejection, and partial-result coverage are asserted. R2 remains incomplete; remaining work includes richer entity transition semantics, counters/resources/health derivations and stronger block-boundary snapshots. No phone or installed-APK behavior was tested.
