package com.portfello.ui.common

import java.text.NumberFormat
import java.util.Currency

/** Locale-aware money formatting, e.g. "1 234,56 zł". Falls back for unknown ISO codes. */
fun formatMoney(value: Double, currency: String): String = try {
    NumberFormat.getCurrencyInstance().apply { this.currency = Currency.getInstance(currency) }
        .format(value)
        // some locales glue alphabetic codes to the number ("PLN123.45") — add the space
        .replace(Regex("(?<=\\p{L})(?=[\\d-])|(?<=\\d)(?=\\p{L})"), " ")
} catch (_: Exception) {
    "%,.2f %s".format(value, currency)
}
