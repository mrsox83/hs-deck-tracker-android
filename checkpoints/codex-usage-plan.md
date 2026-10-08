# Codex usage operating plan

Date established: 2026-10-08.

Purpose: preserve enough of each rolling five-hour allowance to finish a coherent checkpoint, validate it, commit it and push it safely. Usage percentages are live account state, not predictable token budgets, so they must be refreshed during work rather than estimated from prompt size.

Official reference: OpenAI documents that the five-hour ChatGPT Plus allowance is shared across apps using the same plan: <https://developers.openai.com/siwc/token-sharing-open-source/profiles-and-sessions#tracking-usage>.

## Operating thresholds

- Below 65% used: one bounded implementation unit may proceed.
- At or above 65%: do not begin a second implementation unit; finish the current unit and refresh usage.
- At or above 75%: stabilization only—tests, review, documentation, commit and push.
- At or above 80%: do not begin more project work. Preserve and report the current checkpoint.

The five-hour window is the primary scheduling constraint. The weekly window is monitored but does not shorten a safe in-progress checkpoint unless it is close to exhaustion. A full reset credit may be considered when an applicable limit is nearly exhausted, but it requires the user's explicit confirmation each time.

## Checkpoints and meter reads

Refresh live usage:

1. before starting a substantial implementation unit;
2. after a large inspection or implementation pass;
3. before an expensive validation cycle when usage is already near 65%;
4. after validation and before choosing whether to begin another unit.

Do not claim that a task will consume an exact percentage. If the meter moves faster than expected, stop at the next safe boundary and report what remains.

## Safe checkpoint sequence

1. Confirm the requested branch, clean/known working tree, and authorized scope.
2. Select one testable unit with an explicit stopping condition.
3. Implement without merge or deployment.
4. Run the smallest relevant gate first, followed by private or broader gates only when justified.
5. Review the diff for unrelated files and preserved tracker patches.
6. Commit and push the completed checkpoint to `feature/mobile-fusion-offline` when it makes sense.
7. Report the commit, remote verification, tests, remaining uncertainty, and current usage when relevant.

For a push failure, retain the local commit and report the exact command failure, whether it is authentication, authorization, network, non-fast-forward or branch protection, and the specific intervention required. Never merge, deploy, consume a reset credit, or discard user changes as an implicit recovery step.

## Initial planning snapshot

At adoption, the live meter showed 34% of the five-hour window used and 70% of the weekly window used. This is historical context only; always use a fresh reading for decisions.
