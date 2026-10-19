# Growing-prefix patch — independent review

Date: 2026-10-19 Central. Reviewer: local workspace-sharing subagent `/root/growing_prefix_review`. Not CLOUD-001; no persistent agent-management configuration or replacement cloud worker.

Base:3117f992c249e630490a14b2bacf460c42f00072, feature/mobile-fusion-offline. Reviewed uncommitted production patch and relevant tests read-only. Coordinator held production/test changes frozen during review while updating only build-note documentation.

Result: no blocking findings in the bounded correction. Reviewer checked exact completed/non-truncated eligibility, independently linked raw hash/session, canonical full-document content except verified whole-log hash, exact raw-prefix proof, deterministic representative selection, retained original powers/provenance, stripped transient artifact keys, cancellation and saved import immutability. Existing test XML was inspected; reviewer did not rerun tests, open Drive/private raw or validate Android runtime.

Reviewer limitations: Android SHA-256 provider digest-cloning behavior remains a phone gate; unsupported cloning conservatively disables equivalence. Fresh logical-ID policy remains explicitly deferred and is not a regression introduced by this patch. Coordinator's recorded237 ordinary/10 private passes remain the executed test evidence, not new reviewer executions.

## Separate fresh logical-ID policy recommendation

Subsequent owner approval implemented this as a new bounded local unit; the recommendation below is historical design, not the latest implementation status. See checkpoints/mobile-fusion-stable-game-id.md. Review found one malformed-Unicode encoding collision; coordinator added explicit validation and tests. Final review/test disposition is recorded in that checkpoint; no APK or migration is implied.

Current assembly includes every selected source ID in logical match ID, so adding unrelated evidence changes the identifier of a fresh assembly. Repository import ownership already uses saved tracker UUID and preserves an existing FUSED_LOCAL artifact. Recommended next bounded policy: domain/version-tag a new ID derived from the saved tracker UUID, with raw evidence hashes/revisions retained separately as provenance rather than game identity. Do not rewrite, rename, backfill or replace existing artifacts/outbox entries, and do not infer that two different tracker UUIDs are the same real game.

Before implementation, review blank/invalid tracker identity handling, full-digest/truncation collision policy, caller behavior outside the immutable importer, producer-version distinction and compatibility of export/lookup paths. Required tests: same tracker with added/reordered raw sources or edited result retains new-policy ID; different tracker UUIDs remain distinct; exact legacy artifact bytes/path/entry stay unchanged; additional evidence never auto-replaces an accepted artifact. This is a reviewed next-scope recommendation, not an implemented migration or acceptance claim. No APK, commit/push, merge or deployment in this review.
