# Checkpoint: preflight blocked

- Scope: approved R0–R2 only.
- Baseline and current HEAD: `d3bc0fb15c6cf4e29ba75fc0aac54b1ee75541bd`.
- Branch: `feature/mobile-fusion-offline`.
- Source implementation unchanged; existing tracker patches preserved.
- Command execution: passed.
- GitHub read access: passed; write authentication: not established (invalid CLI token/API Forbidden).
- Gradle distribution download: passed using writable cache and session proxy; core test result: passed (dependencies downloaded, core compiled, existing tests ran).
- Real fixtures/exporter source: unavailable; Drive destination denied by network policy.

Resume by restoring configured GitHub authorization and authorized access to the referenced private mobile artifacts, then begin the R0 inventory; retain the successful baseline core gate. Preserve raw artifacts privately and use synthetic/sanitized public tests. Complete source/hash/schema inventory before asserting R0 acceptance. No phone intervention, merge, or deployment is authorized by this checkpoint.

Reproduction command from repository root:

```sh
ANDROID_USER_HOME=/workspace/.android \
GRADLE_USER_HOME=/workspace/.gradle \
JAVA_TOOL_OPTIONS='-Dhttps.proxyHost=proxy -Dhttps.proxyPort=8080 -Dhttp.proxyHost=proxy -Dhttp.proxyPort=8080' \
./gradlew :core:test
```
