# Reddit-Posts für HS Deck Tracker

Zwei fertige Posts zum Selbst-Absenden, geprüft gegen die Regeln der Subreddits (Stand 27.09.2026). Nichts davon ist
bisher gepostet.

## Vor dem Posten

- **Neues Release:** Seit v1.3.0 sind Korrekturen dazugekommen (lesbare Kartennamen im Overlay, Meta-Decks ohne
  falsche fehlende Karten, scrollbare Kartenlisten im Querformat). `versionName` in `app/build.gradle.kts` auf `1.3.1`
  setzen und mergen, dann bekommen neue Nutzer gleich die aktuelle Version.
- **Konto u/stroexd:** angelegt am 26.09.2026, 1 Karma, E-Mail bestätigt. Ein Werbe-Post als erste Aktion eines neuen
  Kontos wird oft automatisch entfernt. Besser vorher ein paar normale Kommentare in r/hearthstone schreiben.
- **Bilder:** Posts mit Screenshots kommen deutlich besser an. Passend sind `docs/overlay.png` (Overlay) und aus
  `fastlane/metadata/android/en-US/images/phoneScreenshots/` die Bilder 2 (Decks mit Staubkosten), 3 (fehlende Karten)
  und 6 (Statistik). Auf reddit.com oder in der App lassen sie sich im Text-Post-Editor einfügen.
- **Zeitpunkt:** abends deutscher Zeit, dann ist auch in Nordamerika Tag.
- **Danach:** auf Kommentare antworten, nichts doppelt posten, Fehlerberichte auf GitHub-Issues verweisen.

## 1. r/hearthstone

- Posten: https://www.reddit.com/r/hearthstone/submit
- Flair: **Community**
- Regeln: Eigenwerbung nur im Verhältnis 9:1 (auf einen eigenen Post etwa 9 normale Beiträge oder Kommentare), kein
  Stream-Link, nichts gegen Blizzards Nutzungsbedingungen. Ein Tracker, der nur den Bildschirm liest, ist unproblematisch.
- Größte Hearthstone-Community (etwa 2 Mio. Mitglieder).

**Titel**

```text
I made a free, open-source deck tracker for Android that runs on the same phone you play on
```

**Text**

```markdown
I play Hearthstone mostly on my phone and always missed a deck tracker there, so I built one.

It reads the game screen with on-device text recognition – no root, no PC, no helper app, and screenshots never leave the phone:

- an overlay in the empty margin next to the board: the cards left in your deck, the opponent's played cards and their likely meta deck (the full tracker in the app adds draw chances)
- game start, both classes, your deck, draws, turns and the result are recognized automatically, and every game lands in a match history with win rates per deck and matchup
- collection sync straight from the in-game collection screen; opened packs and crafting are picked up automatically, so it can show what every deck still costs you in dust
- meta decks from HSReplay, deck builder, deck codes – in English and German, following your game language

On Android 11+ it runs in the background: open Hearthstone and it starts by itself.

Download (APK, own F-Droid repository or Obtainium): https://github.com/stroexd/hs-deck-tracker-android

It's a free hobby project (GPL-3.0, no ads, no account). Recognition still has rough edges on some devices and languages, so bug reports and ideas are very welcome – there's a button for it in the app.
```

## 2. r/droidappshowcase

- Posten: https://www.reddit.com/r/droidappshowcase/submit
- Flair: **Showcase**
- Regeln: höchstens 1 App-Post pro Woche, Link von seriöser Quelle (GitHub passt), keine reinen Link-Posts. Konten unter
  24 Stunden oder mit weniger als 2 Karma werden erst von den Mods geprüft.
- Das Schwester-Subreddit von r/AndroidApps, das selbst keine Eigenwerbung erlaubt.

**Titel**

```text
HS Deck Tracker – free, open-source Hearthstone deck tracker with an in-game overlay (no root)
```

**Text**

```markdown
**What it is**

A deck tracker for Hearthstone that runs on the same phone you play on. It reads the game screen with on-device text recognition – no root, no Shizuku, no PC, no helper app.

**Features**

- Overlay in the empty margin next to the board (left or right, keeps clear of the camera cutout): the cards left in your deck, the opponent's played cards and their likely meta deck
- Automatic tracking: game start, both classes, your deck, draws, turns and the result; match history with win rates per deck and matchup
- Background tracking on Android 11+ through the app's own accessibility service – open Hearthstone and it starts by itself; screen sharing on older versions
- Collection sync from the in-game collection screen; opened packs, crafting and disenchanting are followed automatically, with the dust cost of every deck
- Meta decks from HSReplay.net, deck builder, deck codes, card database; follows your game language (English and German interface)

**Privacy**

Screenshots are processed on the device and never uploaded. No account, no ads, no analytics in the app.

**Download**

- GitHub releases: https://github.com/stroexd/hs-deck-tracker-android/releases/latest
- Or through its own F-Droid repository or Obtainium, with automatic updates: https://github.com/stroexd/hs-deck-tracker-android

Android 8.0+, free and open source (GPL-3.0). Unofficial fan project, not affiliated with Blizzard.

**Feedback**

It's a hobby project and recognition still has rough edges on some devices – bug reports and ideas are very welcome (button in the app, or GitHub issues).
```

## Geprüft und nicht geeignet

| Subreddit | Grund |
|---|---|
| r/AndroidApps | Eigenwerbung verboten, verweist auf r/droidappshowcase |
| r/fossdroid | nur vollständig freie Apps (ML Kit ist proprietär), mit KI geschriebene Apps verboten |
| r/CompetitiveHS | nur Diskussion auf Wettkampfniveau |

## Weitere Orte

- **XDA Forums → Android Apps and Games:** Entwickler-Threads mit Download-Link und Screenshots sind ausdrücklich
  erlaubt; Text wie bei r/droidappshowcase, Titel z. B.
  `[APP][8.0+][FREE][OPEN SOURCE] HS Deck Tracker – Hearthstone deck tracker with overlay, no root`.
- **Hearthstone-Discords:** nur im Kanal für Eigenwerbung (#self-promotion, #showcase o. Ä.), zwei Sätze plus Link.
