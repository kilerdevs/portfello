package com.portfello.data.network

import com.portfello.data.network.model.NbpExchangeRateResponse
import com.portfello.data.network.model.NbpGoldPrice
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
import org.junit.Test

class NbpParsingTest {

    private val moshi = Moshi.Builder().build()

    @Test
    fun `parse exchange rate response`() {
        val json = """
        {
          "table": "A",
          "currency": "dolar amerykański",
          "code": "USD",
          "rates": [
            {"no": "119/A/NBP/2024", "effectiveDate": "2024-06-20", "mid": 4.0350}
          ]
        }
        """.trimIndent()
        val adapter = moshi.adapter(NbpExchangeRateResponse::class.java)
        val resp = adapter.fromJson(json)!!
        assertEquals("USD", resp.code)
        assertEquals(4.0350, resp.rates[0].mid, 0.0001)
        assertEquals("2024-06-20", resp.rates[0].effectiveDate)
    }

    @Test
    fun `parse gold price list`() {
        val json = """[{"data":"2024-06-20","cena":328.45}]"""
        val type = Types.newParameterizedType(List::class.java, NbpGoldPrice::class.java)
        val adapter = moshi.adapter<List<NbpGoldPrice>>(type)
        val prices = adapter.fromJson(json)!!
        assertEquals(1, prices.size)
        assertEquals(328.45, prices[0].price, 0.001)
        assertEquals("2024-06-20", prices[0].date)
    }
}
