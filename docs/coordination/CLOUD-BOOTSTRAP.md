# Cloud bootstrap — coordinator-issued launch capsule required

Do not launch with unresolved placeholders. READY_NOT_DISPATCHED is a reservation, not permission to self-start. Coordinator fills the following after repository environment verification and records DISPATCHED in TASKS:

```text
Task: CLOUD-001
Repository: mrsox83/hs-deck-tracker-android
Environment: <verified repository cloud environment>
START_SHA: <exact40hex published coordination commit>
Worker/chat ID: <one assigned worker>
Task branch: <coordinator-assigned isolated branch>
Review/expiry time: <UTC timestamp; expiry never authorizes a second worker>

Verify repo and checkout START_SHA on the assigned task branch, clean state.
Read AGENTS.md, docs/coordination/TASKS.md, HANDOFF.md and tasks/CLOUD-001.md.
Confirm this capsule matches the coordinator-issued assignment before edits.
Do not use main, a floating latest branch, local H: paths or another worker's checkout.
Work only within the three allowlisted summary/report paths in the contract.
Implement the passive renderer and synthetic tests, not app integration or reducer fixes.
Do not edit shared interfaces, build files, global plans/logs or task ownership.
No private captures/signing keys/API keys needed; request no such access.
Run specified ordinary tests; honestly report toolchain blockers and unrun gates.
Return the assigned report plus reviewable patch/commit, base SHA and changed files.
No integration-branch push, merge, release, deployment or recursive delegation.
If scope/base/ownership conflicts or required evidence is unavailable, stop and report.
```

Coordinator accepts/rejects the returned diff, runs local private/device gates where applicable, then commits/pushes accepted work to feature/mobile-fusion-offline. Cloud result is not self-integrating.

## Environment preparation checklist

Choose a real Codex Cloud environment connected to this repository, not the generic coaching ChatGPT project. Verify access to the feature branch and launch SHA. Configure existing pinned toolchain/dependency access without signing secrets; no APK build required. Run a baseline ordinary core test before publishing/dispatch. Keep private fixtures local; use synthetic repo data. If the cloud has no approved environment/network setup, report BLOCKED rather than secretly changing project config.

Official setup/instruction references: https://learn.chatgpt.com/docs/environments/cloud-environments and https://learn.chatgpt.com/docs/agent-configuration/agents-md . Product access is not proof a particular repository environment is configured. No cloud worker has been dispatched merely by committing this file.
