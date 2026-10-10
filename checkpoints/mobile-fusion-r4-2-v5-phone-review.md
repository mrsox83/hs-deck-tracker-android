# R4.2 v5 phone evidence review

Diagnostic (11), 8,948,694 bytes, arrived in the existing private Drive Diagnostic directory. Its 100 entries include 50 AUTOMATIC_ROUTE records and nine new manual bookmarks, all KEYFRAME_SAVED with media_projection backend and landscape-pilot-2 metadata. All 95 retained image SHA256 values and byte sizes match. Raw evidence and extracted previews remain outside Git. These are owner-classified testing games, excluded from coaching by the shared R4 policy.

Visual review in bookmark order:

| Bookmark | Visible friendly mana | Visible adjacent counter | Review |
| --- | --- | --- | --- |
| 1 | 0/2 | 1 | Expanded hand; friendly hero obscured. |
| 2 | 1/1 | 0 | Clear board/normal hand; OCR reused, candidates withheld. |
| 3 | 1/1 | 0 | Expanded hand; OCR reused, candidates withheld. |
| 4 | 2/2 | 0 | Expanded hand; opponent health 30 agrees with candidate. |
| 5 | 1/2 | 0 | Expanded hand; opponent health 30 agrees with candidate. |
| 6 | 0/6 | 8 | Expanded hand; opponent health 15 and adjacent 8 agree. |
| 7 | 10/10 | 11 | Expanded hand; adjacent 11 agrees. Opponent health visibly 15, OCR unknown. |
| 8 | 3/10 | 16 | Expanded hand plus enlarged card display; adjacent 16 agrees. Opponent health visibly 18, OCR unknown. |
| 9 | Not gameplay | Not gameplay | Menu screen; no gameplay anchor, all candidates withheld. |

These are human image labels, not accepted semantic tracker events. Stored mana remains unknown in every new bookmark: current exact same-line pair rule cannot read the stacked/partially missing resource digits. No values are inferred from crystals or earlier frames. Expanded-hand cue behaves consistently on fresh gameplay OCR; reused OCR cannot establish a current layout. Occluded friendly health is unknown. Adjacent resource values are still deliberately untyped in source, although the owner identifies this Death Knight resource as corpses.

Cost-icon observation: focused-hand cards have distinct silhouettes around their cost. Reanimated Pterrordax visibly states “Costs Corpses instead” and uses a different cost badge from ordinary mana-cost cards. Blood Draw also shows a distinct red badge. This supports retaining cost icon/cost type as a future bounded visual hypothesis, not reading a numeric cost alone as mana or assigning health-payment semantics without a labeled fixture. Cost discounts, animation/glow and card rotation require negative cases.

Acceptance: retained v5 screen-share bookmarks and new source/layout metadata are phone-observed. Settings removal/automatic Accessibility routing are not independently confirmed by this archive. No additional capture test is needed for this finding. R4.2 remains open. Next finite implementation proposal: geometry-aware independent numerator/denominator candidates for the two known resource layouts, validated on this corpus and rejecting missing/merged/reused OCR; keep cost-icon classification deferred until labeled examples. Surface the R4.3 transition explicitly after this bounded resource unit instead of expanding indefinitely. No R4.3/R5, merge or deployment.
