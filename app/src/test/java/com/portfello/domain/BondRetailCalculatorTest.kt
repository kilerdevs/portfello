package com.portfello.domain

import com.portfello.data.db.entity.BondRetailDetails
import org.junit.Assert.assertEquals
import org.junit.Test

class BondRetailCalculatorTest {

    private fun details(rate: Double, cap: String) = BondRetailDetails(
        assetId = 1, nominal = 100.0, series = "ROR0725",
        interestRate = rate, rateUpdatedAt = 0, capitalizationType = cap
    )

    @Test
    fun `annual capitalization - 1 year at 6 pct`() {
        val d = details(6.0, "ANNUAL")
        val oneYear = 365.25 * 24 * 3600 * 1000L
        val value = BondRetailCalculator.estimatedValue(d, 0L, oneYear.toLong())
        assertEquals(106.0, value, 0.5)
    }

    @Test
    fun `annual capitalization - 2 years at 6 pct`() {
        val d = details(6.0, "ANNUAL")
        val twoYears = (2 * 365.25 * 24 * 3600 * 1000L).toLong()
        val value = BondRetailCalculator.estimatedValue(d, 0L, twoYears)
        // 100 * 1.06^2 = 112.36
        assertEquals(112.36, value, 0.5)
    }

    @Test
    fun `end capitalization - simple interest`() {
        val d = details(5.0, "END")
        val twoYears = (2 * 365.25 * 24 * 3600 * 1000L).toLong()
        val value = BondRetailCalculator.estimatedValue(d, 0L, twoYears)
        // 100 * (1 + 0.05 * 2) = 110
        assertEquals(110.0, value, 0.5)
    }

    @Test
    fun `zero time returns nominal`() {
        val d = details(6.0, "ANNUAL")
        val value = BondRetailCalculator.estimatedValue(d, 1000L, 1000L)
        assertEquals(100.0, value, 0.01)
    }
}
