package com.stroexd.hsdecktracker.core

import com.stroexd.hsdecktracker.core.cards.*
import com.stroexd.hsdecktracker.core.deck.*
import java.io.File
import kotlin.test.*

class RealCardCatalogFixtureTest {
    @Test fun `full downloaded catalog parses and exact deck resolves all cards`() {
        val db = CardDatabase.parse(File(requireNotNull(System.getProperty("hsFullCardCatalog"))).readText())
        val definition = DeckCode.decode("AAECAeeKBxSXoASh1ASQ5ASR5ASH9gSI9gSy9wTV5QbW5QbIjAe8lAf1mAftnwf3qgf7qgf8qgfSrgessAeOvweI3QcF9eMEupUHkasHtdoHptwHAAA=")
        assertEquals(30, definition.cardCount)
        definition.cards.keys.forEach { assertNotNull(db.byDbfId(it), "Unknown imported ID $it") }
        assertEquals("Bwonsamdi", db.byDbfId(120187)?.name)
        assertEquals("What Befell Zandalar", db.byDbfId(120188)?.name)
        assertTrue(db.recognitionCards.any { it.dbfId == 120187 })
        assertTrue(db.recognitionCards.any { it.dbfId == 120188 })
        assertTrue(db.deckCards.none { it.dbfId in setOf(120187, 120188) })
        println("Full catalog: ${db.size} entries; ${db.deckCards.size} deck-builder cards; ${db.recognitionCards.size} recognition cards")
    }
}
