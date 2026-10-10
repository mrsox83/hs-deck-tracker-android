package com.stroexd.hsdecktracker.core

import com.stroexd.hsdecktracker.core.cards.*
import com.stroexd.hsdecktracker.core.collection.*
import com.stroexd.hsdecktracker.core.data.CardRepository
import com.stroexd.hsdecktracker.core.data.HttpClient
import com.stroexd.hsdecktracker.core.deck.*
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.*
import java.nio.file.Files
import kotlin.test.*

class FullCardCatalogTest {
    private val code = "AAECAeeKBxSXoASh1ASQ5ASR5ASH9gSI9gSy9wTV5QbW5QbIjAe8lAf1mAftnwf3qgf7qgf8qgfSrgessAeOvweI3QcF9eMEupUHkasHtdoHptwHAAA="
    // Minimal public catalog fixture: actual IDs/names, not private match evidence.
    private val associated = listOf(
        Card(120187, "TIME_619t", "Bwonsamdi", cost = 6, type = "MINION", cardClass = "DEATHKNIGHT", set = "TIME_TRAVEL"),
        Card(120188, "TIME_619t2", "What Befell Zandalar", cost = 3, type = "SPELL", cardClass = "DEATHKNIGHT", set = "TIME_TRAVEL"),
    )
    private val collectibleJson = """[{"dbfId":1,"id":"TEST_1","name":"Test collectible","type":"MINION","collectible":true,"set":"TIME_TRAVEL"}]"""
    private val fullJson = collectibleJson.dropLast(1) + """,{"dbfId":120187,"id":"TIME_619t","name":"Bwonsamdi","type":"MINION","set":"TIME_TRAVEL"},{"dbfId":120188,"id":"TIME_619t2","name":"What Befell Zandalar","type":"SPELL","set":"TIME_TRAVEL"}]"""

    @Test fun `exact owner deck code preserves associated IDs and counts without unknown warnings`() {
        val decoded = DeckCode.decode(code)
        assertEquals(30, decoded.cardCount)
        assertEquals(1, decoded.cards[120187])
        assertEquals(1, decoded.cards[120188])
        val db = CardDatabase(decoded.cards.keys.filterNot { it in setOf(120187, 120188) }.map {
            Card(it, "FIXTURE_$it", "Fixture $it", type = "MINION", collectible = true, set = "TIME_TRAVEL")
        } + associated)
        val deck = Deck(name = "Example Death Knight Deck", heroClass = HsClass.DEATHKNIGHT, cards = decoded.cards, sideboards = decoded.sideboards)
        assertTrue(DeckAnalysis.validate(deck, db, FormatRules()).none { it is DeckIssue.UnknownCard })
        val roundTrip = DeckCode.decode(DeckCode.encode(decoded))
        assertEquals(decoded.cards, roundTrip.cards)
        assertEquals(decoded.sideboards, roundTrip.sideboards)
        assertEquals(decoded.heroes, roundTrip.heroes)
        assertEquals(30, deck.cardCount)
        assertEquals("Bwonsamdi", db.byDbfId(120187)?.name)
        assertEquals("What Befell Zandalar", db.byDbfId(120188)?.name)
        assertTrue(db.recognitionCards.containsAll(associated))
        assertTrue(db.deckCards.none { it.dbfId in setOf(120187, 120188) })
        assertTrue(db.cardsInSet("TIME_TRAVEL").none { !it.collectible })
        val crafting = CraftingCalculator.analyze(mapOf(120187 to 1, 120188 to 1), emptyList(), CardCollection(), db)
        assertTrue(crafting.missing.isEmpty())
        assertEquals(0, crafting.dustCost)
        assertTrue(associated.none { it.isCraftable })
        assertFalse(Card(9, "BG_TEST", type = "MINION", set = "BATTLEGROUNDS").isRecognitionCard)
    }

    @Test fun `legacy cache is preserved upgraded despite same version and full cache works offline`() = runTest {
        val dir = Files.createTempDirectory("full-card-catalog").toFile()
        dir.resolve("cards_enUS.json").writeText(collectibleJson)
        dir.resolve("cards_enUS.version").writeText("same-build")
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse().setHeader("ETag", "same-build"))
            server.enqueue(MockResponse().setBody(fullJson))
            val url = server.url("/v1/latest/%s/cards.json").toString()
            val repository = CardRepository(dir, HttpClient(), withSetNames = false, cardsUrl = url)
            repository.load("enUS")
            assertNotNull(repository.db.byDbfId(120187))
            assertNotNull(repository.db.byDbfId(120188))
            assertEquals(collectibleJson, dir.resolve("cards_enUS.json").readText())
            assertTrue(dir.resolve("cards_full_enUS.json").exists())
            assertEquals("/v1/latest/enUS/cards.json", server.takeRequest().path)
            assertEquals("GET", server.takeRequest().method)
            server.enqueue(MockResponse().setResponseCode(503))
            val offline = CardRepository(dir, HttpClient(), withSetNames = false, cardsUrl = url)
            offline.load("enUS")
            assertNotNull(offline.db.byDbfId(120187))
            assertTrue(offline.db.deckCards.all { it.collectible })
        }
        assertTrue(CardRepository.CARDS_URL.endsWith("/cards.json"))
    }

    @Test fun `failed migration retains usable legacy data and corrupt full cache falls back`() = runTest {
        val dir = Files.createTempDirectory("failed-card-upgrade").toFile()
        dir.resolve("cards_enUS.json").writeText(collectibleJson)
        dir.resolve("cards_full_enUS.json").writeText("broken")
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse().setResponseCode(503))
            server.enqueue(MockResponse().setResponseCode(503))
            val repository = CardRepository(dir, HttpClient(), withSetNames = false,
                cardsUrl = server.url("/v1/latest/%s/cards.json").toString())
            repository.load("enUS")
            assertNotNull(repository.db.byDbfId(1))
            assertNotNull(repository.state.value.error)
            assertEquals(collectibleJson, dir.resolve("cards_enUS.json").readText())
        }
    }
}
