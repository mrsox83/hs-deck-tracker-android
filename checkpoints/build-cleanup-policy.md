# User-requested post-build cleanup

After completed builds or large file-change batches, clean disposable project build intermediates/tmp using scripts/Cleanup-BuildTemps.ps1 (dry-run by default, -Apply to remove). Do not run while builds/tests are active. Inspect build-process ownership before stopping any idle process; never kill unrelated apps or promise RAM gains from deleting disk files.

Preserve APK outputs, test reports/results, deliveries, toolchain/dependency caches, private fixtures, diagnostic evidence and logs. Do not clear all Windows Temp or synced source trees. Generated files removed by this helper are rebuildable, not backed up. Locked files must be reported rather than security software bypassed. No memory-store changes made.
