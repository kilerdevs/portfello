package com.portfello.domain

import com.portfello.data.db.entity.Asset
import com.portfello.data.db.entity.AssetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfitLossTest {

    private fun valuation(totalValue: Double, costBasis: Double?) = AssetValuation(
        asset = Asset(type = AssetType.STOCK, name = "Test"),
        totalValue = totalValue,
        baseCurrency = "PLN",
        pricePerUnit = null,
        priceCurrency = null,
        lastUpdated = null,
        costBasisInBase = costBasis
    )

    @Test
    fun `gain computed against cost basis`() {
        val v = valuation(totalValue = 1200.0, costBasis = 1000.0)
        assertEquals(200.0, v.profitLoss!!, 1e-9)
        assertEquals(20.0, v.profitLossPct!!, 1e-9)
    }

    @Test
    fun `loss is negative`() {
        val v = valuation(totalValue = 800.0, costBasis = 1000.0)
        assertEquals(-200.0, v.profitLoss!!, 1e-9)
        assertEquals(-20.0, v.profitLossPct!!, 1e-9)
    }

    @Test
    fun `null cost basis yields no PL`() {
        val v = valuation(totalValue = 800.0, costBasis = null)
        assertNull(v.profitLoss)
        assertNull(v.profitLossPct)
    }

    @Test
    fun `zero cost basis yields no percentage`() {
        val v = valuation(totalValue = 800.0, costBasis = 0.0)
        assertEquals(800.0, v.profitLoss!!, 1e-9)
        assertNull(v.profitLossPct)
    }
}
