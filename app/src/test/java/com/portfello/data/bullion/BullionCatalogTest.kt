package com.portfello.data.bullion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BullionCatalogTest {

    @Test
    fun `catalog has all 24 coins`() {
        assertEquals(24, BullionCoinCatalog.coins.size)
    }

    @Test
    fun `filter by metal works`() {
        val gold = BullionCoinCatalog.byMetal("Au")
        assertTrue(gold.all { it.metal == "Au" })
        assertEquals(10, gold.size)
    }

    @Test
    fun `silver eagles weight and purity`() {
        val eagle = BullionCoinCatalog.coins.first { it.name == "Amerykański Orzeł (Ag)" }
        assertEquals(31.11, eagle.totalWeightGrams, 0.01)
        assertEquals(0.999, eagle.purity, 0.001)
    }

    @Test
    fun `sovereign specs`() {
        val sov = BullionCoinCatalog.coins.first { it.name == "Suweren" }
        assertEquals(7.98, sov.totalWeightGrams, 0.01)
        assertEquals(0.9167, sov.purity, 0.0001)
    }
}
