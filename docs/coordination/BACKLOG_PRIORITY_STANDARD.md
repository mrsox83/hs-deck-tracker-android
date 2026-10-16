# HS-FUSION backlog priority standard

Owner-approved governance for all coaching, development, validation and automation reports. This standard controls queue order; a reporter's urgency language does not.

## Authority and separation

- A reporting agent records observations, evidence and a **provisional severity**. It may recommend investigation, but it may not assign or change canonical priority, reorder the queue, authorize implementation, or mark acceptance complete.
- The HS-FUSION coordinator assigns **scheduling priority** only after comparing the report with the complete active backlog. The human owner may override any assignment.
- Severity describes impact if the report is accurate. Priority describes what should be worked next. They are separate fields and must not be substituted for each other.
- Until coordinator triage, use `PRIORITY: UNASSESSED`. Never infer P0/P1 from words such as critical, urgent, blocking, highest, or must-fix in reporter text.

## Required bug-report fields

1. Shared HSC ID or `NEW-ID-NEEDED`.
2. Reporter, observation time, app/build/source identity and affected workflow.
3. Expected behavior and directly observed behavior; distinguish reproduction from interpretation.
4. Reproduction rate and scope: exact fixture/device/account/session when known, plus what remains unknown.
5. Impact categories: safety/privacy, irreversible data loss, source/provenance correctness, wrong user-visible result, blocked core workflow, performance, or UX.
6. Workaround availability and reliability.
7. Evidence links/hashes/log lines/screenshots and contradictions with existing evidence.
8. Dependencies, regression risk and proposed acceptance test.
9. `REPORTER SEVERITY: S0-S4` with a one-sentence rationale; `PRIORITY: UNASSESSED` unless the coordinator has triaged it.

Missing fields do not make a report low importance. They make it incomplete and route it to bounded triage.

## Severity scale

- **S0 — catastrophic:** confirmed or strongly evidenced security/privacy exposure, irreversible user-data loss, destructive corruption, or broadly unusable product with no safe workaround.
- **S1 — major:** core workflow blocked, exported/saved truth materially wrong, or provenance/identity corrupted, with no reliable workaround or broad affected scope.
- **S2 — significant:** important behavior is wrong or incomplete, but impact is bounded or a reliable workaround exists.
- **S3 — minor:** limited UX, presentation, or low-risk operational defect; core evidence and workflow remain trustworthy.
- **S4 — observation/enhancement:** improvement request, speculative concern, or insufficiently demonstrated defect.

Severity may be raised immediately when new evidence establishes greater impact. Lowering it requires evidence, not absence of additional reports.

## Scheduling priority scale

- **P0 — stop the line:** coordinator/owner assigned only for an active S0 condition or equivalent immediate release/data-integrity threat. Pause conflicting implementation and define containment first.
- **P1 — next bounded unit:** verified S1, a prerequisite blocking multiple accepted tasks, or a time-sensitive regression with no safe workaround.
- **P2 — planned:** verified S2 or contained S1 with a reliable workaround; schedule against dependencies and validation capacity.
- **P3 — backlog:** S3, narrow/low-frequency defect, or work with an adequate workaround and no near-term dependency.
- **P4 — parked:** enhancement, historical unknown without actionable evidence, duplicate, or intentionally deferred item.

## Coordinator comparison order

Before assigning or changing priority, compare the candidate with every active P0-P2 item in this order:

1. Safety, privacy, security and irreversible data loss.
2. Corruption of saved/exported truth, identity, chronology or provenance.
3. Core workflow availability and release-blocking regressions.
4. Breadth, frequency and reproducibility on the current supported build.
5. Whether the item blocks other accepted work or validation.
6. Workaround reliability and reversibility.
7. Time sensitivity and affected-user scope.
8. Effort and convenience only as tie-breakers; a quick fix does not automatically outrank higher-impact work.

Coaching usefulness, strategic value, novelty, or reporter confidence cannot by themselves outrank correctness, provenance, data preservation, security, or an active release blocker.

## Triage and change record

Every coordinator priority decision records:

- assigned severity and priority;
- evidence reviewed and important unknowns;
- the active higher/equal-priority items it was compared against;
- why it does or does not preempt current work;
- owner override, if any;
- next bounded action and acceptance evidence.

Priority changes retain the previous value and rationale. New evidence may trigger re-triage; reporter emphasis alone may not. `RESOLVED`, `DONE` and acceptance states still require their separate evidence gates.
