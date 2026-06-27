package com.portfello.data.network

import com.portfello.data.repository.Quote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PriceCacheTest {

    @Test
    fun `put and get returns cached quote`() {
        val cache = PriceCache()
        val quote = Quote(4.05, "PLN", "NBP")
        cache.put("test:USD:PLN", quote)
        val result = cache.get("test:USD:PLN")
        assertNotNull(result)
        assertEquals(4.05, result!!.price, 0.001)
    }

    @Test
    fun `expired entry returns null`() {
        val cache = PriceCache()
        val quote = Quote(4.05, "PLN", "NBP")
        cache.put("test:USD:PLN", quote, ttlMs = 1)
        Thread.sleep(5)
        assertNull(cache.get("test:USD:PLN"))
    }
}
