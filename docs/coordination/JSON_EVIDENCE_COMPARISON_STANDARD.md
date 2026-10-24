# JSON evidence comparison standard

Documentation-only procedure, approved October23,2026. Do not change app hashing, artifact IDs, existing evidence or serialization merely to make a comparison pass.

1. Preserve original files and record each exact byte length/SHA-256. A matching hash is BYTE-IDENTICAL; a mismatching hash is not automatically corruption or a changed game.
2. Parse both complete documents with a strict, schema-aware reader. Reject malformed/truncated input and ambiguous duplicate object keys. Do not silently coerce number/string/boolean/null types, drop unknown fields, fill defaults, reorder arrays, deduplicate events or rewrite values. If the reader cannot preserve a distinction, report comparison UNRESOLVED rather than equal.
3. Compare all parsed fields and values, preserving array order and duplicates. Any substantive difference is CONTENT-DIFFERENT and must be described, not normalized away. Ignore only JSON presentation differences that do not change those values (for example whitespace or object-member layout).
4. If parsed content agrees, report CONTENT-EQUIVALENT, BYTE-DIFFERENT. This is a separate claim, never an exact-file match. To reconcile a provenance hash, reproduce the actual producer's known serializer/options, field order and UTF-8 bytes; arbitrary minification or generic sorted-key serialization is not sufficient. Only a reproduced expected hash establishes that serialization relation.
5. Keep all original hashes, the reproduced hash, serializer/encoding evidence and limits in the validation record. Never replace a source-file hash with a normalized hash. If expected serializer/hash cannot be reproduced, leave provenance-byte reconciliation unresolved even when parsed content agrees.

## Verified phone example

PostMatchDiscovery serializes the saved MatchRecord with compact AppJson for fusion intake; MatchJsonExport uses PrettyJson for standalone export. The October23 phone tracker export SHA-25613bb44bd88b093bc2cb574cea45a957cf351b6b22147e8956fe68bcceb7c59fd differs from fusion tracker-source hash97aa8efe4eb08a1e96b0fbe6ba2ba2298a52adf75119693d8b97426391cb4574. Compact UTF-8 serialization in the original field order reproduces the latter exactly; pretty indent4 reproduces the former exactly. The tracker timeline also agrees exactly,59 observations in order. This demonstrates this record's formatting relation, not a universal canonical JSON algorithm. Keep raw ZIP, Power.log and accepted match-document hashes independently verified. No app patch, rebuild or re-upload is required for this presentation difference.
