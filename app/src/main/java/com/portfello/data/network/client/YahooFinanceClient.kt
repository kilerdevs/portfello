package com.portfello.data.network.client

import com.portfello.data.network.HttpClientProvider
import com.portfello.data.network.NetworkLog
import com.portfello.data.network.withRetry
import com.portfello.data.repository.HistoryPoint
import com.portfello.data.repository.HistoryProvider
import com.portfello.data.repository.PriceProvider
import com.portfello.data.repository.Quote
import com.portfello.data.repository.SearchProvider
import com.portfello.data.repository.SearchResult
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YahooFinanceClient @Inject constructor(
    private val http: HttpClientProvider,
    private val networkLog: NetworkLog
) : PriceProvider, HistoryProvider, SearchProvider {

    private val moshi = Moshi.Builder().build()

    override suspend fun getPrice(id: String, currency: String): Result<Quote> = runCatching {
        withRetry { fetchChart(id, "1d", "5d") }
    }

    override suspend fun getHistory(id: String, currency: String, days: Int): Result<List<HistoryPoint>> = runCatching {
        val range = when {
            days <= 7 -> "7d"
            days <= 30 -> "1mo"
            days <= 90 -> "3mo"
            days <= 365 -> "1y"
            else -> "5y"
        }
        val json = withRetry { getJson("https://query1.finance.yahoo.com/v8/finance/chart/$id?interval=1d&range=$range") }
        parseHistory(json, id)
    }

    override suspend fun search(query: String): Result<List<SearchResult>> = runCatching {
        val url = "https://query2.finance.yahoo.com/v1/finance/search?q=${query}&quotesCount=8&newsCount=0"
        val json = withRetry { getJson(url) }
        parseSearch(json)
    }

    private suspend fun fetchChart(ticker: String, interval: String, range: String): Quote {
        val url = "https://query1.finance.yahoo.com/v8/finance/chart/$ticker?interval=$interval&range=$range"
        val json = getJson(url)
        val chart = json.asMap("chart") ?: throw Exception("Yahoo: no chart data")
        val results = chart.asList("result") ?: throw Exception("Yahoo: no results")
        if (results.isEmpty()) throw Exception("Yahoo: empty results for $ticker")
        val result = results[0] as? Map<*, *> ?: throw Exception("Yahoo: bad result format")
        val meta = result.asMap("meta") ?: throw Exception("Yahoo: no meta")
        val price = meta.asDouble("regularMarketPrice") ?: throw Exception("Yahoo: no price for $ticker")
        val cur = meta.asString("currency") ?: "USD"
        return Quote(price = price, currency = cur, source = "Yahoo")
    }

    private fun parseHistory(json: Map<String, Any?>, ticker: String): List<HistoryPoint> {
        val chart = json.asMap("chart") ?: return emptyList()
        val results = chart.asList("result") ?: return emptyList()
        if (results.isEmpty()) return emptyList()
        val result = results[0] as? Map<*, *> ?: return emptyList()
        val meta = result.asMap("meta") ?: return emptyList()
        val cur = meta.asString("currency") ?: "USD"
        val timestamps = (result["timestamp"] as? List<*>)?.mapNotNull { (it as? Number)?.toLong() } ?: return emptyList()
        val indicators = result.asMap("indicators") ?: return emptyList()
        val quoteList = indicators.asList("quote") ?: return emptyList()
        if (quoteList.isEmpty()) return emptyList()
        val quote = quoteList[0] as? Map<*, *> ?: return emptyList()
        val closes = (quote["close"] as? List<*>)?.map { (it as? Number)?.toDouble() } ?: return emptyList()

        return timestamps.zip(closes).mapNotNull { (ts, close) ->
            if (close == null) return@mapNotNull null
            HistoryPoint(timestamp = ts * 1000, price = close, currency = cur)
        }
    }

    private fun parseSearch(json: Map<String, Any?>): List<SearchResult> {
        val quotes = json.asList("quotes") ?: return emptyList()
        return quotes.mapNotNull { item ->
            val q = item as? Map<*, *> ?: return@mapNotNull null
            val symbol = q.asString("symbol") ?: return@mapNotNull null
            val name = q.asString("shortname") ?: q.asString("longname") ?: symbol
            val exchange = q.asString("exchDisp") ?: q.asString("exchange") ?: ""
            SearchResult(id = symbol, name = name, symbol = symbol, exchange = exchange)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun getJson(url: String): Map<String, Any?> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url)
            .header("Accept", "application/json")
            .build()
        val response = http.client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Yahoo HTTP ${response.code}")
        val body = response.body?.string() ?: throw Exception("Yahoo empty body")
        moshi.adapter(Map::class.java).fromJson(body) as? Map<String, Any?> ?: throw Exception("Yahoo: invalid JSON")
    }

    // ponytail: manual map access — avoids Moshi data classes for a volatile API
    private fun Map<*, *>.asMap(key: String) = this[key] as? Map<String, Any?>
    @Suppress("UNCHECKED_CAST")
    private fun Map<*, *>.asList(key: String) = this[key] as? List<Any?>
    private fun Map<*, *>.asString(key: String) = this[key]?.toString()
    private fun Map<*, *>.asDouble(key: String) = (this[key] as? Number)?.toDouble()

    companion object {
        const val SILVER_FUTURES = "SI=F"
        const val PLATINUM_FUTURES = "PL=F"
        const val PALLADIUM_FUTURES = "PA=F"
    }
}
