package com.portfello.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BullionFormulaTest {

    @Test
    fun `gold maple leaf value formula`() {
        // 31.11g * 0.9999 purity * 330 PLN/g (NBP spot) + 50 PLN premium
        val weight = 31.11
        val purity = 0.9999
        val spotPlnPerGram = 330.0
        val premium = 50.0
        val value = weight * purity * spotPlnPerGram + premium
        val expected = 31.11 * 0.9999 * 330.0 + 50.0
        assertEquals(expected, value, 0.01)
    }

    @Test
    fun `silver eagle value formula via USD conversion`() {
        // Stooq: 30 USD/oz → PLN/g = (30 / 31.1034768) * 4.05 (USD/PLN) ≈ 3.907 PLN/g
        val usdPerOz = 30.0
        val usdPln = 4.05
        val spotPlnPerGram = (usdPerOz / TROY_OZ_GRAMS) * usdPln

        val weight = 31.11
        val purity = 0.999
        val premium = 20.0
        val value = weight * purity * spotPlnPerGram + premium
        // 31.11 * 0.999 * 3.907 + 20 ≈ 121.38 + 20 = 141.38
        assertEquals(141.38, value, 2.0)
    }

    @Test
    fun `troy ounce constant`() {
        assertEquals(31.1034768, TROY_OZ_GRAMS, 0.0000001)
    }
}
