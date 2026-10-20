# Observer and reveal timing — existing raw audit

Date: 2026-10-20 Central. Status: AUDIT COMPLETE / KNOWLEDGE NOT CERTIFIED. Owner approved read-only inspection of existing first-player, second-player and rewind uploads. Repository baseline c28ad9d19ff5c8a90e4b41fb8aa0809fa49b6fd6, feature/mobile-fusion-offline. No product code, schema, APK, saved artifact, upload or source publication changed. Prior local review documentation and untracked helper preserved.

## Method and limits

Opened three pinned ZIPs read-only, verified wrapper and inner Power.log SHA256, then checked all66896 selected parsed event bodies against their referenced raw lines. Read three tracker files with pinned hashes and checked only first-player flags. Card IDs/names and player aliases were compared privately in memory; outputs contain numeric entity/controller references and structural counts only, never account/player/card contents. No gameplay judgment, network research or new phone capture.

Repeatable ignored local helper: .gradle/observer-audit/Invoke-ObserverAudit.ps1, SHA256109cfab27a6196210a7c186edaeb0f16df4a25f2f0a869fb989851df91ebbc1c. Its output is an evidence inventory, not a production parser or test suite. It mirrors reducer-supported FULL/SHOW/CHANGE/block-descriptor identity observation paths for timing counts, then separately spot-checks other earlier raw descriptors. Counts of identities absent from that tracked history are NOT counts of identities absent from all raw data or from human knowledge.

Scope: exactly three source captures, not universal log semantics. No markers from the searched LOCAL_PLAYER/LOCAL_CONTROLLER/IS_LOCAL/IS_FRIENDLY/OBSERVER/SPECTATOR/AUDIENCE/VISIBLE_TO/CLIENT_PLAYER families were found (case-insensitive variants, including common ID suffixes). This is a bounded negative search, not proof no other format could identify an observer. Player/account records exist, but no authenticated local-account anchor was established; account IDs were not emitted. Raw account/player record counts include repeated logging streams and are not unique-person counts.

## Evidence matrix

| Profile | Selected event bodies checked | SHOW / HIDE | SHOW with unchanged tracked identity | Submitted-choice associations |
|---|---:|---:|---:|---|
| FIRST, tracker wentFirst=true | 22601 | 137 / 104 | 18 | 7 same-epoch offer/send joins, all offered controller2 |
| SECOND, tracker wentFirst=false | 21580 | 97 / 125 | 4 | 8 joins, all controller1 |
| REWIND, tracker wentFirst=false | 22715 | 93 / 42 | 1 | 9 joins, all controller2 under original alias bindings; post-reset trust unproven |

### Observer association: useful candidate, not verified audience

FIRST has explicit alias-to-controller raw mappings at512/513. Choice2 offer at913 resolves exactly to controller2; SendChoices at1071 and server-chosen record1074 reference the same choice. All seven submitted-choice headers join to controller2 offers. SECOND mappings640/641, choice1 offer1045, SendChoices1519 and chosen1522 similarly associate controller1. Alias values remain private. These joins use exact preceding mappings, not result, class, first-player flag or turn parity.

REWIND mappings509/510, choice2 offer869, send1065 and chosen1068 associate controller2 initially. GAME_RESET lines are3573,8246,18970,29121,37618. The same alias/controller correlation persists across offers and sends in epochs1–5, but the only alias-binding records located are epoch0. Audit correlation may reuse those strings; a production knowledge gate must NOT silently carry their authority across reset. No renewed capture-perspective proof was established.

Important selected checkpoint: #84 BEFORE_ACTION34937 is in epoch4, after reset29121 but before that epoch's offer36408/send36577. A same-epoch submitted-choice rule cannot use those future records to certify observer knowledge at34937. The eight hand positions remain supported independently; observer mapping/names/costs remain unestablished there unless a separate continuity proof is approved and demonstrated.

SendChoices headers have no explicit numeric observer field. Association comes from joining the submitted choice to its earlier offer and resolving its Player alias. That supports a DERIVED/CANDIDATE acting-controller association under the logging-method interpretation; it is not, by itself, authenticated capture ownership, spectator-mode exclusion or evidence of which private information a human could see. An additional rule review/validation is required before calling it VERIFIED observer knowledge.

### SHOW/HIDE and repeated identities

No searched observer/audience/recipient fields appear in the327 SHOW records. SHOW records include DECK-zone entities belonging to both controllers: FIRST1081(owner2)/1376(owner1), SECOND1536(owner1)/1597(owner2), REWIND1075(owner2)/1216(owner1). Thus SHOW plus a known card ID cannot alone establish public or selected-observer availability; the raw evidence does not define that audience.

Direct repeated-value witnesses: SECOND entity23 has SHOW1562, HIDE42606, same-identity SHOW46994 in epoch0. REWIND entity61 has SHOW1237, HIDE1458, same-identity SHOW1460 in epoch0. FIRST entity160 has source identity10722, HIDE10752, same-value SHOW16693. These establish repeated source records, not necessarily uninterrupted card-instance continuity or human knowledge retention. A HIDE marker neither proves human forgetting nor licenses unchecked identity carry through transforms/shuffles/reuse.

The reducer suppresses unchanged identityHistory entries; repeated SHOW therefore cannot be discovered reliably from identityHistory alone. Its boolean visibility history retains changes but no observer audience. A future gate needs reveal-event provenance/semantics, not a final identity or a boolean visibility shortcut.

### Reveal timing and BEFORE_ACTION

The audit found4/91 FIRST,4/93 SECOND and31/68 REWIND named outer PLAY/ATTACK boundaries where the action identity was not already equal in the reducer-compatible tracked identity history. These counts describe that history model only. Many identities already occurred in earlier raw TAG_CHANGE descriptors or unsupported raw forms, so they must not be reported as newly known to the source or human at those boundaries.

Concrete witnesses:

- FIRST entity55 PLAY1775, epoch0: no earlier same-identity entity descriptor was found in the selected stream. Reducer observes that actor descriptor before same-line BEFORE_ACTION capture. An unrestricted HistoricalIdentity <=1775 join would therefore include an identity introduced by the action boundary itself. Current position-only output is safe because identities remain null.
- SECOND entity14 PLAY2437: tracked history does not contain the matching identity earlier, but raw NUM_TURNS_IN_HAND descriptor2186 already contains it. This distinguishes reducer-history absence from raw-source absence; neither establishes human ignorance.
- REWIND entity28 ATTACK5229, epoch1: same-identity CARD_TARGET descriptor5228 immediately precedes it at the same logged time; another matching descriptor exists3765. A blanket '< snapshot line' predicate is not a demonstrated decision-knowledge rule. The earlier source identity still needs audience/continuity evidence; the adjacent action-related record is not itself proof of prior decision-time availability or selected targeting intent.

Earlier same-identity SHOW records before later actions also exist, e.g. FIRST28520 -> PLAY28653; SECOND52228 -> PLAY52356; REWIND2546 -> PLAY2764. Their existence does not establish a trusted observer audience or a certified decision boundary. Do not discard legitimate prior knowledge, but do not promote these joins into it without the missing prerequisites.

## Proposed next bounded implementation — not authorized by this audit

Recommend a core-only in-memory observer-association resolver, NOT enabling known-card coaching yet. Consume existing raw-derived choice and alias records; require one unambiguous same-source/same-epoch offer/send join and alias evidence available by the requested cutoff. Return DERIVED/CANDIDATE controller plus exact evidence, or explicit unknown/conflict. Never infer local=1, use future submissions at earlier checkpoints, reuse choice IDs across epochs, or carry alias authority through resets/reconnects without separately supported continuity. Do not claim a numeric candidate proves visibility.

Keep all hand names/private costs withheld and expected completeness unknown. No FusedMatch persistence/schema extension, uploader/UI, new dependency, hidden-card inference or artifact rewrite in this next unit. Tests should pin both opposite controller cases, missing/ambiguous aliases and offers, both sides submitting, future-only evidence, repeated choice IDs across resets, unsupported continuity and named-action timing leakage boundaries. Use the existing three raw witnesses locally; no new uploads or game requested. Review before publication; APK remains separate.

Positive card knowledge and private costs still require a separately validated audience/reveal/continuity rule and a certified pre-decision boundary. If the candidate association semantics cannot be established, leave it unknown and propose the smallest necessary capture metadata rather than manufacturing verification. This audit does not authorize that capture change.

## Pinned provenance

- FIRST bundle HS-export-20261007-142822-44f30f8a.zip:6e89dacbe05f040a10ab5a73ac79e2edc31934d2c3e3fbf18d94a981ff038d4e. Inner Power.log:2c05f4bf27aca34e440ef4fc3a554759ffe40605f01beaa9bdf3193b52b38744. Tracker hash:d9b037917de5007d456b305850d3dd9e6ddaf919d59b8e2928d08de31057d574.
- SECOND bundle HS-export-20261008-161122-04f747f4.zip:3034236b5d61e1f75d0ace581710952cbc1c969ed9c9fec25ec4a2f6b649e92e. Inner Power.log:b3f2f5c912db77f821b47cfa2611d32ba51366c686813eb4646c831c06ea0ed4. Tracker hash:57c9e135926c2811cae9cbd83bc5a18f71b1bc13eb9c8ca4979f5c1c7b4a771f.
- REWIND bundle HS-game-20261018-165407-e3bc4eb7-logs.zip:9392b19815be64f0e053b1edc50933d5bd06749a311369ad0e09da4b21bd8d69. Inner Power.log:7729f57c8893047fcdb360892323b28031523d52bdf259f714e28730cfef6f0a. Tracker hash:0540889ea13d924e1be148cc1e4a41029c466c1a31ac4823dc3328e3268e7004.

No new core test suite or APK executed; previous254 ordinary/11 private passes remain earlier evidence, not this audit's result. No canonical backlog/Resource Exchange writes, priority changes, commits/pushes, merge/deployment, or broad HSC closure. Usage18%five-hour/61%weekly at audit readout, ceiling90%; no reset used.
