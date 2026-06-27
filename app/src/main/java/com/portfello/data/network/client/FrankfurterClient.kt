package com.portfello.data.network.client

import com.portfello.data.network.HttpClientProvider
import com.portfello.data.network.model.FrankfurterResponse
import com.portfello.data.network.withRetry
import com.portfello.data.repository.HistoryPoint
import com.portfello.data.repository.HistoryProvider
import com.portfello.data.repository.PriceProvider
import com.portfello.data.repository.Quote
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FrankfurterClient @Inject constructor(
    private val http: HttpClientProvider
) : PriceProvider, HistoryProvider {

    private val moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(FrankfurterResponse::class.java)
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override suspend fun getPrice(id: String, currency: String): Result<Quote> = runCatching {
        val url = "https://api.frankfurter.app/latest?from=$id&to=$currency"
        val json = withRetry { get(url) }
        val resp = adapter.fromJson(json) ?: throw Exception("Frankfurter parse error")
        val rate = resp.rates[currency] ?: throw Exception("Frankfurter: no rate for $currency")
        Quote(price = rate, currency = currency, source = "Frankfurter")
    }

    override suspend fun getHistory(id: String, currency: String, days: Int): Result<List<HistoryPoint>> = runCatching {
        val end = LocalDate.now()
        val start = end.minusDays(days.toLong())
        val url = "https://api.frankfurter.app/${start.format(fmt)}..${end.format(fmt)}?from=$id&to=$currency"
        val json = withRetry { get(url) }
        // Frankfurter time series is {base, start_date, end_date, rates: {date: {CUR: val}}}
        // Parse manually since it's a nested map
        val map = moshi.adapter<Map<String, Any>>(Map::class.java).fromJson(json) ?: throw Exception("parse error")
        @Suppress("UNCHECKED_CAST")
        val rates = map["rates"] as? Map<String, Map<String, Double>> ?: return@runCatching emptyList()
        rates.entries.mapNotNull { (dateStr, curMap) ->
            val price = curMap[currency] ?: return@mapNotNull null
            val date = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: return@mapNotNull null
            HistoryPoint(
                timestamp = date.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli(),
                price = price,
                currency = currency
            )
        }.sortedBy { it.timestamp }
    }

    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        val response = http.client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Frankfurter HTTP ${response.code}")
        response.body?.string() ?: throw Exception("Frankfurter empty body")
    }
}
