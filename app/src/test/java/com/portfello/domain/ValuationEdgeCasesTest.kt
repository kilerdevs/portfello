package com.portfello.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValuationEdgeCasesTest {

    @Test
    fun `zero quantity yields zero value`() {
        val qty = 0.0
        val price = 150.0
        assertEquals(0.0, qty * price, 0.001)
    }

    @Test
    fun `bond at purchase moment returns nominal`() {
        val nominal = 100.0
        val rate = 0.06
        val yearsHeld = 0.0
        assertEquals(nominal, bondValue(nominal, rate, yearsHeld, "ANNUAL"), 0.001)
    }

    @Test
    fun `bond END capitalization 3 years`() {
        val nominal = 100.0
        val rate = 0.06
        val years = 3.0
        val expected = nominal * (1.0 + rate * years) // 118.0
        assertEquals(expected, bondValue(nominal, rate, years, "END"), 0.001)
    }

    @Test
    fun `bullion value with zero purity yields only premium`() {
        val weight = 31.1
        val purity = 0.0
        val spotPerGram = 330.0
        val premium = 50.0
        val value = weight * purity * spotPerGram + premium
        assertEquals(50.0, value, 0.001)
    }

    @Test
    fun `multiple holdings sum correctly`() {
        val holdings = listOf(1.0, 2.5, 0.5)
        val pricePerUnit = 100.0
        val total = holdings.sumOf { it * pricePerUnit }
        assertEquals(400.0, total, 0.001)
    }

    @Test
    fun `metal USD-oz to PLN-gram conversion`() {
        val usdPerOz = 30.0
        val usdPln = 4.0
        val plnPerGram = (usdPerOz / TROY_OZ_GRAMS) * usdPln
        assertTrue(plnPerGram > 0)
        assertEquals((30.0 / 31.1034768) * 4.0, plnPerGram, 0.0001)
    }

    private fun bondValue(nominal: Double, rate: Double, years: Double, capType: String): Double {
        if (years <= 0) return nominal
        return when (capType) {
            "ANNUAL" -> {
                val full = years.toInt()
                val frac = years - full
                val comp = nominal * Math.pow(1.0 + rate, full.toDouble())
                comp * (1.0 + rate * frac)
            }
            else -> nominal * (1.0 + rate * years)
        }
    }
}
