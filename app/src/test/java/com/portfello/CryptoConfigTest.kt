package com.portfello

import com.portfello.data.crypto.CryptoConfig
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CryptoConfigTest {

    private val moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(CryptoConfig::class.java)

    @Test
    fun `config round-trips through json`() {
        val config = CryptoConfig(
            salt = "dGVzdHNhbHQ=",
            pinHash = "dGVzdGhhc2g="
        )
        val json = adapter.toJson(config)
        val restored = adapter.fromJson(json)
        assertNotNull(restored)
        assertEquals(config.salt, restored!!.salt)
        assertEquals(config.pinHash, restored.pinHash)
        assertEquals(3, restored.argonTimeCost)
        assertEquals(65536, restored.argonMemoryCost)
    }
}
