package com.portfello.data.network

import com.portfello.data.network.model.CoinGeckoMarketChart
import com.portfello.data.network.model.CoinGeckoSearchResponse
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Test

class CoinGeckoParsingTest {

    private val moshi = Moshi.Builder().build()

    @Test
    fun `parse search response`() {
        val json = """
        {
          "coins": [
            {"id": "bitcoin", "name": "Bitcoin", "symbol": "btc", "market_cap_rank": 1},
            {"id": "bitcoin-cash", "name": "Bitcoin Cash", "symbol": "bch", "market_cap_rank": 20}
          ]
        }
        """.trimIndent()
        val resp = moshi.adapter(CoinGeckoSearchResponse::class.java).fromJson(json)!!
        assertEquals(2, resp.coins.size)
        assertEquals("bitcoin", resp.coins[0].id)
        assertEquals("btc", resp.coins[0].symbol)
    }

    @Test
    fun `parse market chart`() {
        val json = """{"prices":[[1718841600000,260000.50],[1718928000000,261500.75]]}"""
        val chart = moshi.adapter(CoinGeckoMarketChart::class.java).fromJson(json)!!
        assertEquals(2, chart.prices.size)
        assertEquals(1718841600000.0, chart.prices[0][0], 0.1)
        assertEquals(260000.50, chart.prices[0][1], 0.01)
    }
}
