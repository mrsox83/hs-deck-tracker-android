# Credential audit — 2026-10-14

## Conclusion

The reported GitGuardian Google Gemini API-key alert is a verified false positive at the linked location. No actual credential literal or private signing key was found within the scanned scope. This is a bounded audit result, not a guarantee that no credential exists anywhere or that provider accounts have never been misused.

Feature development was paused for this audit. No merge, deployment, release workflow execution, credential rotation, history rewrite, or reset-credit consumption occurred.

## Exact alert and provenance

The owner authorized reading the GitGuardian email. It reports a push at 2026-10-12 19:35:37 UTC and links commit `597fb7980e1d1e2afc26b2285dad9f5a3f485435`, former line 16 of `checkpoints/mobile-fusion-r5-modal-export.md`:

https://github.com/mrsox83/hs-deck-tracker-android/commit/597fb7980e1d1e2afc26b2285dad9f5a3f485435#diff-30bd0d91a4ddd9e0c948554689c223a6e9075a93ad4e93d4b9312c907323bb99R16

That release paragraph contained two public integrity values:

- Delivered manual-v5 APK SHA-256: `33ea23f368e30d881281974be3249848a5bfb0470b7ddfd6a6c4f311a1790271`. Independently recomputed from the 68,206,454-byte delivered APK.
- Signing-certificate SHA-256 fingerprint: `4cf267d92ed361c32e2d4baa302670ee6c62a0cad042586e43de684ccc18f380`. Independently verified with Android apksigner on that APK. A public certificate fingerprint is not the private signing key.

These values entered Git intentionally as release-verification evidence. Neither is a Gemini credential or grants API/signing access. The email does not identify the detector's exact matched substring or internal cause. Misclassification of this hash-bearing paragraph is the supported inference; detector internals were not inspected. No GitGuardian dismissal or account modification was performed.

## Scope and evidence

Verified the mrsox83/hs-deck-tracker-android repository and feature/mobile-fusion-offline branch. Audit baseline local and remote tip: `ee30893165e44f0a54f7a594ce589c3246af21c9`. Remote refs were fetched. Original tracker patches remain ancestors; no main edits.

Post-hardening scan covered current source, configuration, documents, generated outputs, local private helper scripts, all local mobile-fusion delivery directories, reachable Git refs and reflog objects, and decompressed APK/JAR/AAR/ZIP entries. Final counts: **15,400 files, 945 historical blobs, 77 archives, 20,720 archive entries; zero read errors**. No detected credential literals/private-key headers or sensitive key-container archive entries. Four historical workflow blobs matched a credential-in-URL variable template, not an actual committed token.

The separately identified unreachable blob `87bdbde76fe9626c306f8e0da7fc446a7639b136` (9,407 bytes) also had no Google/OpenAI/GitHub credential or private-key-header pattern hit. No Git objects were deleted.

Rules checked recognizable Google AIza, OpenAI sk-, GitHub token prefixes, AWS access-key IDs, Slack tokens, PEM private-key headers, text credential assignments and credential-bearing URLs. Binary data used strict prefix/header rules; archives additionally checked sensitive filenames. Scanner output includes locations/rules only, never matched credential values. The private scanner stays outside Git.

The only tracked signing container is inherited `signing/keys.p12.enc`, introduced by upstream commit `9e5702b`. Its 8,256-byte file has the OpenSSL Salted__ encrypted-container header. The inherited workflow specifies AES-256-CBC, PBKDF2 and 600,000 iterations with the password sourced from Actions secrets. This audit did not decrypt it, verify password strength, or prove the encrypted contents/key ownership. The personal Android debug keystore remains local, untracked and unchanged. Selected current-process signing/GitHub-token variables were absent; environment values were not dumped.

## Preventive changes, not live-secret removal

1. Removed the inherited debug.keystore ignore exception and added ignores for JKS/private-key/environment/service-account files. Verified representative paths are ignored. This reduces accidental future staging; it does not remove an already tracked file or prevent forced adds.
2. Changed the inherited release workflow from an expanded token-bearing Git push URL to GitHub CLI's credential helper (`gh auth setup-git --hostname github.com`) and a token-free HTTPS URL. The former source stored `$GH_TOKEN`, not a literal secret. At runtime the old form could expose a job token through process arguments, diagnostic command reporting, or failure output, subject to runner masking/access controls. No actual runtime disclosure was established. Existing ephemeral token/permissions are unchanged; only disposable-runner credential delivery changes. No workflow run or F-Droid publication occurred.
3. Split and explicitly labeled the public APK checksum and certificate fingerprint in the flagged checkpoint. Preserved their exact values and integrity provenance. No private information was deleted from that paragraph because none was present.

## Risk and limits

The reported hashes themselves create no credential-access risk and need no rotation or history purge. No confirmed live secret was found, so this audit provides no evidence of exposed API quota, billing access, account takeover, or signing impersonation. Provider billing/access logs and GitGuardian's authenticated detector detail were not inspected; absence of misuse cannot be concluded from a code scan alone.

If a different alert identifies a real key, treat its historical exposure as compromised: revoke/rotate it with the issuing provider, review scope and access/billing logs, remove current copies, then consider coordinated history cleanup. Deleting a current line alone would not revoke an exposed key. Do not rotate the personal signing key casually: changing it can prevent updates to the installed test app.

Excluded directories: .git filesystem internals (object contents inspected separately), .gradle, .kotlin, .private-toolchain, node_modules and symlinks. Files above 128 MiB, archive entries above 64 MiB, and archives deeper than two nested levels were excluded. Generic encrypted/obfuscated/custom-format credentials and entropy-only candidates are not exhaustively detected. Unrelated repositories, whole-drive private files, cloud secret settings, provider logs and remote CI logs were not exhaustively audited. Private gameplay fixtures remain outside Git.

## Verification and handoff

Postpatch scanner completed with the counts above. `git diff --check` passed. Ignore behavior passed. Official GitHub CLI documentation confirms the helper command and hostname option. Local PyYAML was unavailable; no YAML-parser pass or release-workflow execution is claimed. No Android source changed, so no new APK was built or phone test required. Source-ready and static checks passed; CI runtime validation remains unperformed.

Live usage at audit wrap-up: five-hour 30% used, seven-day 12% used. No reset used. Development remains paused until the security report is handed off; existing overlay/discovery phone work remains queued, not silently completed.
