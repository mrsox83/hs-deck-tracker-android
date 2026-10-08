# Mobile fusion work log

## 2026-10-07 — preflight

User approved continuation of R0–R2 using the attached implementation plan. The attachment's stale approval-pending wording is not a new approval requirement. No implementation changes have been made.

Shell execution succeeded. Initial checkout was `work` at `de24d36d48d3cddc4162f966d60f9918faec7611`. Remote lookup and fetch verified `feature/mobile-fusion-offline` at `d3bc0fb15c6cf4e29ba75fc0aac54b1ee75541bd`; the local feature branch now points there. Its four patches include inferred friendly plays, hand-gap recovery, OCR feedback-loop correction, and manual match-export recovery.

GitHub API repository access returned Forbidden; `gh auth status` reports the configured GH_TOKEN is invalid. Git read access works. `git push --dry-run` against the existing exact branch head returned Everything up-to-date, which does not establish new-commit write authorization. No remote refs were changed.

The first core-test invocation failed because the default Gradle cache under `/home/agent` is unwritable. Using `GRADLE_USER_HOME=/workspace/.gradle` fixed the cache location. Java then failed direct DNS resolution for services.gradle.org. Supplying JVM HTTP/HTTPS proxy properties for the inherited session proxy allowed Gradle 8.14.3 to download successfully. The Android plugin also needed `ANDROID_USER_HOME=/workspace/.android` to avoid the read-only default Android home. With both writable cache locations and proxy settings, dependency downloads, core compilation, and `:core:test` passed (BUILD SUCCESSFUL). Nonfatal fontconfig/ONNX cache warnings occurred. This validates existing tests, not the unavailable real fusion fixtures.

No configured secrets or outbound identities were reported by the cloud runtime. Access to the plan's Drive exporter source URL was denied by the proxy (CONNECT 403). Required private source/fixture artifacts are absent locally. No attempt was made to bypass the network policy; factual summaries in the plan are not substitutes for raw fixtures or independent labels.

Only planning/checkpoint documentation was added. No app/core implementation, merge, or deployment was performed.

Baseline test report totals: tests=94, failures=0, errors=0, skipped=0. Existing diagnostic tests may return early without external fixtures; a green baseline does not establish fixture coverage.

## 2026-10-07 — R0 complete, R1/R2 core started

Applied the user-supplied checkpoint patch on `feature/mobile-fusion-offline` after independently verifying the remote branch remained at `d3bc0fb15c6cf4e29ba75fc0aac54b1ee75541bd`. The checkpoint commit is local only. No merge, push or deployment was performed.

Authorized access to `H:\My Drive\HSReplay` is now available. Inspected the v0.4 exporter source, all current mobile export bundles and tracker JSON fixtures, and independently read the representative Rafaam pair. Exact inventory hashes and source/build uncertainty are in `checkpoints/mobile-fusion-r0.md`. Raw private artifacts were not copied into the repository.

Added a pure-JVM `hs-fused-match/1` core: immutable provenance-bearing claims and sources, tracker and Power evidence adapters, deterministic replay deduplication, conservative pairing with ambiguous/composite rejection, ordered nested action blocks, time-scoped entity/tag history, unknown-tag retention, separate choice stages, boundary snapshots, and compact source pointers. This is offline-only and does not change tracker capture, persistence, legacy export, Android permissions or UI.

Added five synthetic fusion tests covering nested ordering/provenance, unknown tags and incomplete blocks, accepted and ambiguous pairing, shuffled/repeated import, and composite rejection. The complete `:core:test` suite passed using JDK 17 and cached dependencies. Real-fixture acceptance remains open; synthetic success is not a substitute for the private-fixture assertions requested by R1/R2.
