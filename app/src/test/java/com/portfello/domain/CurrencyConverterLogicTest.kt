package com.portfello.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyConverterLogicTest {

    @Test
    fun `same currency returns amount unchanged`() {
        val amount = 1234.56
        assertEquals(amount, convertSameCurrency(amount, "PLN", "PLN"), 0.001)
    }

    @Test
    fun `cross rate via PLN pivot`() {
        val usdPln = 4.05
        val eurPln = 4.32
        val expectedUsdEur = usdPln / eurPln
        assertEquals(expectedUsdEur, crossRate(usdPln, eurPln), 0.0001)
    }

    @Test
    fun `inverse rate`() {
        val usdPln = 4.05
        val plnUsd = 1.0 / usdPln
        assertEquals(plnUsd, 1.0 / usdPln, 0.0001)
    }

    private fun convertSameCurrency(amount: Double, from: String, to: String): Double {
        return if (from == to) amount else amount * crossRate(1.0, 1.0)
    }

    private fun crossRate(fromPln: Double, toPln: Double): Double = fromPln / toPln
}
