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

## Boundary

This is one independently asserted real pair, not complete R1 or R2 acceptance. Remaining work includes broad bundle inventory reduction, incomplete/truncated and multi-match fixture assertions, richer entity transition semantics, counters/resources/health derivations, block-boundary snapshots, and negative real pairing cases. No phone or installed-APK behavior was tested.
