# Owner-authorized card catalog fix — 2026-10-10

## Phone acceptance

After installing v7, owner explicitly confirmed both names resolved, the deck remained at 30 cards and saved history stayed intact (“Yes, all good”). The bounded catalog fix is phone-accepted. No claim of general OCR accuracy, measured performance or unrelated v6 field-assessment acceptance. Implementation eb43342 was pushed and independently remote-verified. No merge/deployment/R5.

Separate bounded bug fix after R4 closeout, not discretionary R4 expansion or R5 work. Owner supplied exact “warm dead” Death Knight deck export and authorized the fix. Starting remote-verified tip 15d3efe, branch feature/mobile-fusion-offline; preserve the four tracker patches and all existing data.

Root cause: CardRepository fetched cards.collectible.json. Deck imports explicitly contain Bwonsamdi (120187 / TIME_619t) and What Befell Zandalar (120188 / TIME_619t2), which exist in full HearthstoneJSON but have no collectible flag. Filtering the lookup catalog made the imported IDs unknown. Removing them from the deck would corrupt its exported content.

Fix: fetch full cards.json for lookup; constructed gameplay recognition includes associated/non-collectible cards, while deck-builder/search/collection lists remain collectible-only. Menu/collection OCR uses its own collectible-only index. Non-collectibles never create missing crafting requirements or dust costs. No deck code/count/sideboard/history rewrite.

Cache migration: new cards_full_<locale>.json/version profile forces a download even at the same game build. Keep the old cards_<locale>.json/version unchanged as offline fallback. Invalid full cache falls back to valid legacy data; failed download leaves existing usable data and visible error. New full cache works offline. No reinstall/delete required; internet is needed once to obtain the full catalog.

Verification: 144 ordinary tests passed, including exact owner code round-trip (30 cards; both associated IDs one copy), lookup/filter/crafting separation and safe cache migration/failure/offline cases. Required downloaded-catalog gate passed on 36,022 entries: 7,406 deck-builder cards and 14,010 constructed recognition cards. All imported IDs resolve. Download SHA256 e8ea569a7ca1790c2258ed57c60798d808ffb057acb7bb73663c00ea174dae65; raw catalog remains outside Git. This finite result does not promise general OCR accuracy or measured phone memory/performance. Larger catalog/recognition index phone behavior remains unverified.

APK build/publication details in WORK_LOG. Phone gate: update only Fusion R4 Test, open online once, then open existing warm dead deck and check both names replace Unknown card without altering count/history. No game/capture campaign or deck re-import required. Optional manual card-data refresh if needed. Report any remaining warning verbatim rather than deleting cards. No merge/deployment/R5.
