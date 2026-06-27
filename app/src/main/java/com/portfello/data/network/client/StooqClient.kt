package com.portfello.data.network.client

import com.portfello.data.network.HttpClientProvider
import com.portfello.data.network.NetworkLog
import com.portfello.data.network.withRetry
import com.portfello.data.repository.HistoryPoint
import com.portfello.data.repository.HistoryProvider
import com.portfello.data.repository.PriceProvider
import com.portfello.data.repository.Quote
import com.portfello.data.repository.SearchResult
import com.portfello.data.repository.SearchProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StooqClient @Inject constructor(
    private val http: HttpClientProvider,
    private val networkLog: NetworkLog
) : PriceProvider, HistoryProvider, SearchProvider {

    private val dateFmt = DateTimeFormatter.ofPattern("yyyyMMdd")

    override suspend fun getPrice(id: String, currency: String): Result<Quote> = runCatching {
        withRetry { fetchCurrentQuote(id) }
    }

    override suspend fun getHistory(id: String, currency: String, days: Int): Result<List<HistoryPoint>> = runCatching {
        val end = LocalDate.now()
        val start = end.minusDays(days.toLong())
        val url = "https://stooq.com/q/d/l/?s=$id&d1=${start.format(dateFmt)}&d2=${end.format(dateFmt)}&i=d"
        val csv = withRetry { get(url) }
        parseCsvHistory(csv, id)
    }

    override suspend fun search(query: String): Result<List<SearchResult>> = runCatching {
        // ponytail: Stooq has no search API — suggest ticker variants with market suffixes
        val q = query.trim().lowercase()
        val candidates = if (q.contains('.')) {
            listOf(q)
        } else {
            listOf("$q.us", q, "$q.uk", "$q.de")
        }
        val results = mutableListOf<SearchResult>()
        for (ticker in candidates) {
            try {
                val quote = fetchCurrentQuote(ticker)
                val label = when {
                    ticker.endsWith(".us") -> "US"
                    ticker.endsWith(".uk") -> "UK"
                    ticker.endsWith(".de") -> "DE"
                    else -> "GPW"
                }
                results.add(SearchResult(
                    id = ticker,
                    name = "${query.uppercase()} ($label)",
                    symbol = ticker.uppercase(),
                    exchange = label
                ))
            } catch (_: Exception) { /* skip unavailable */ }
        }
        if (results.isEmpty()) {
            listOf(SearchResult(id = q, name = query.uppercase(), symbol = query.uppercase(), exchange = "Stooq"))
        } else results
    }

    private suspend fun fetchCurrentQuote(ticker: String): Quote {
        val end = LocalDate.now()
        val start = end.minusDays(7)
        val url = "https://stooq.com/q/d/l/?s=$ticker&d1=${start.format(dateFmt)}&d2=${end.format(dateFmt)}&i=d"
        val csv = get(url)
        val lines = csv.trim().lines()
        if (lines.size < 2) throw Exception("Stooq: no data for $ticker")
        val header = lines[0].split(",")
        val closeIdx = findColumn(header, "Close", "Zamkniecie")
        if (closeIdx == -1) {
            networkLog.log("Stooq CSV header: ${lines[0]}")
            throw Exception("Stooq: no Close column in [${header.joinToString()}]")
        }
        val lastLine = lines.last()
        val raw = lastLine.split(",").getOrNull(closeIdx) ?: throw Exception("Stooq: missing Close value")
        if (raw == "N/D" || raw.isBlank()) throw Exception("Stooq: no data for $ticker")
        val close = raw.toDoubleOrNull() ?: throw Exception("Stooq: invalid close price '$raw'")
        if (close == 0.0) throw Exception("Stooq: zero price for $ticker")
        val cur = guessStooqCurrency(ticker)
        return Quote(price = close, currency = cur, source = "Stooq")
    }

    private fun parseCsvHistory(csv: String, ticker: String): List<HistoryPoint> {
        val lines = csv.trim().lines()
        if (lines.size < 2) return emptyList()
        val header = lines[0].split(",")
        val dateIdx = findColumn(header, "Date", "Data")
        val closeIdx = findColumn(header, "Close", "Zamkniecie")
        if (dateIdx == -1 || closeIdx == -1) {
            networkLog.log("Stooq history CSV header: ${lines[0]}")
            return emptyList()
        }
        val cur = guessStooqCurrency(ticker)
        return lines.drop(1).mapNotNull { line ->
            val cols = line.split(",")
            val date = runCatching { LocalDate.parse(cols[dateIdx]) }.getOrNull() ?: return@mapNotNull null
            val price = cols[closeIdx].toDoubleOrNull() ?: return@mapNotNull null
            HistoryPoint(
                timestamp = date.atStartOfDay(ZoneId.of("Europe/Warsaw")).toInstant().toEpochMilli(),
                price = price,
                currency = cur
            )
        }
    }

    private fun findColumn(header: List<String>, vararg names: String): Int {
        for (name in names) {
            val idx = header.indexOfFirst { it.equals(name, ignoreCase = true) }
            if (idx != -1) return idx
        }
        return -1
    }

    private fun guessStooqCurrency(ticker: String): String {
        val t = ticker.lowercase()
        return when {
            t.endsWith(".us") -> "USD"
            t.endsWith(".uk") -> "GBP"
            t.endsWith(".de") -> "EUR"
            t.startsWith("xau") || t.startsWith("xag") || t.startsWith("xpt") || t.startsWith("xpd") -> "USD"
            else -> "PLN"
        }
    }

    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        val response = http.client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Stooq HTTP ${response.code}")
        response.body?.string() ?: throw Exception("Stooq empty body")
    }

    companion object {
        const val GOLD_USD_TICKER = "xauusd"
        const val SILVER_USD_TICKER = "xagusd"
        const val PLATINUM_USD_TICKER = "xptusd"
        const val PALLADIUM_USD_TICKER = "xpdusd"
    }
}
