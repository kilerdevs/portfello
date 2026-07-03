package com.portfello.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class FormatMoneyTest {

    private fun <T> withLocale(locale: Locale, block: () -> T): T {
        val old = Locale.getDefault()
        Locale.setDefault(locale)
        try { return block() } finally { Locale.setDefault(old) }
    }

    @Test
    fun `alphabetic code is separated from digits`() = withLocale(Locale.US) {
        assertEquals("PLN 2,058,106.65", formatMoney(2_058_106.65, "PLN"))
    }

    @Test
    fun `negative value keeps space after code`() = withLocale(Locale.US) {
        val s = formatMoney(-123.45, "PLN")
        assertTrue(s, s.contains("PLN 123.45") || s.contains("PLN -123.45"))
    }

    @Test
    fun `symbol currencies are untouched`() = withLocale(Locale.US) {
        assertEquals("$1,234.50", formatMoney(1234.5, "USD"))
    }

    @Test
    fun `unknown code falls back`() = withLocale(Locale.US) {
        assertEquals("1,234.50 XYZ99", formatMoney(1234.5, "XYZ99"))
    }
}
