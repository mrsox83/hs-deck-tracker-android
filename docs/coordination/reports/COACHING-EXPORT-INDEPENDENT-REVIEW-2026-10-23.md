# Independent coaching export source review — October23

## Focused correction re-review — original findings addressed

Owner approved bounded fixes. Pairing anchors now require PLAY, explicit incorporated source, event epoch/cutoff, observed actor controller matching evaluated side, and contemporaneous identity; event/identity/owner proofs retained. HandHistory validation compares the complete export to canonical derivation, rejecting changed actor/target/cards/costs/refs/actions on encode and decode. Archive recursion clears supplemental fields. Reviewer re-inspected these exact source changes/new synthetic cases read-only and found original paths closed, no new bounded blocker; ran no tests/private reads. Canonical validation adds one reconstruction, runtime cost unmeasured. Additional cutoff/source negative coverage remains useful.

Current coordinator tests:286 ordinary pass, zero failures/errors/skips; unchanged exact M.O.T.H.E.R. private test and Android Kotlin compile pass. Existing broader12 private tests remain9 pass/3 fail: supplemental-only proof, Priest PENDING→ACCEPTED, descriptor totals. Goldens unchanged. Priest candidate stillscore5/endpoint11s/three scoped PLAY anchors after correction; other candidate score0 rejected endpoint/first-player. Needs exact raw witnesses, not an automatic flip to PASS. Command overall BUILD FAILED1m58s at broad gate. Reports .gradle/export-review-fixes-20261023/core/test-results/{test,realCoachingHandPackageTest,realFusionFixtureTest}. No APK/publication/canonical closure.

Reviewed corrected SHA256: FusionPairing.kt C06ECF63BE4DFC0D3E52266FB103A3D7F8417888040885D119727A67528E21D9; FusionProvenanceValidator.kt AF3A5B5B7F4B2CBF91403D15D995D3B7CEBD1F2BECF94042460350A5709EF1C4. Earlier fingerprints/findings below are historical, not current unresolved claims. Usage75%five-hour/34%weekly under80%; reserve remaining quota for handoff, no further test loops this window.

Owner explicitly approved one independent read-only review sub-agent. Reviewer /root/coaching_export_review inspected source/synthetic tests at HEAD1203094e5f0ca44e157db812d1ec33f944397d0b with uncommitted changes, made no edits, ran no tests/builds and accessed no private logs, attachments, .gradle reports, H:, external services or signing files. No delegation. Coordinator supplied test outcomes are not independently executed evidence. Source was frozen during review. No publication/APK acceptance.

## Finding1 — unsupported historical pairing anchors

Reporter severity S1; PRIORITY UNASSESSED, coordinator triage pending. New descriptor/restoration identity observations update final entity.cardId (PowerEvidenceReducer.kt:136 and97). Existing PowerPairingFacts.fromReduced (FusionPairing.kt:32–34) resolves every historical event actor via final match.entities, without epoch/cutoff/controller/kind checks, and cites event evidence without the identity proof. A later identity on entity44 can become an earlier CARD_A anchor; entity ID reuse across resets can map historical actors to a current identity. Two anchors plus endpoint agreement can reach ACCEPTED. This exposes a pre-existing weakness; private Priest PENDING→ACCEPTED must not be dismissed as a stale golden. Reviewer did NOT prove the private Priest case exercises this specific failure.

Suggested bounded correction: resolve identity at each event's exact source/epoch/cutoff, retain identity proof, constrain actor event/controller eligibility explicitly. Synthetic tests for later identification, reset ID reuse and opponent actor anchors. No implementation authorized by reviewer and no golden changes made.

## Finding2 — derived hand export values insufficiently validated

Reporter severity S2; PRIORITY UNASSESSED. FusionProvenanceValidator.kt:25–43 compares checkpoint metadata and checks reference locations but does not validate actorCardBeforeAction/targetCardBeforeAction; does not verify card identity/cost/position/controller/cost-change values against canonical checkpoint state; action link only requires any outer action in the same epoch. A valid synthetic history can replace its targetCardBeforeAction with an arbitrary unsupported card, or alter a cardId while retaining old evidence, and encode still accepts it. Unincorporated-source references can satisfy the generic reference helper.

Suggested bounded correction: validate derived fields against canonical projection and exact outer-action boundary, including actor/target association, source cutoff and epoch. Negative encode/decode tests for fabricated cards/values, future or unincorporated refs and unrelated actions. Existing success round-trip tests do not cover this boundary.

## Additional performance observation

FusionProvenanceValidator.kt:129 recursively validates epoch archives while retaining handHistory on the validation copy. Each recursion unnecessarily walks every checkpoint and generates errors against cleared snapshots/events before discarding those errors. Clear supplemental fields or use an entity-only helper. Static redundant work established; runtime impact not measured.

## Bounded positive observations and limitations

FULL_ENTITY routing clears stale recipients for unsupported headers and routes explicit-ID restored tags. Builder uses source cutoffs/epoch archives and checks cost against materialized state, retaining unknowns. UI removal/button substitutions show no source blocker; optional fields have old-decoding defaults. Fallback-proof/audit golden changes are plausible but require coordinator raw-witness reconciliation, not reviewer approval. Broader private suite remains9/12. No no-blocker result or canonical acceptance implied.

## Reviewed source fingerprints, SHA256

- CoachingHandHistory.kt: CEE08B10225706ABE14362A94A16BBA36AA6C83E50E23B0BB9072F9BD2FD03ED
- PowerEvidenceReducer.kt: 035699721526265E875ED2567541B44F35AE6E0FED669A7F458EC92805428DAB
- FusionPairing.kt: 898363C06F02B4042592E8D795CC40D78D534AD812C98F75FDC52AE9046928E9
- FusionProvenanceValidator.kt: 243E9A0579CDCDA4F875B2A8F9A8B3809ABD70B13FC53F2A66D2EB835617C7B1

Next coordinator/owner gate: approve bounded corrections1/2 and validation efficiency fix, then rerun ordinary/private gates and request focused re-review before publication/APK. Preserve existing broad-suite goldens until evidence reconciliation. No code changed during this review task.
