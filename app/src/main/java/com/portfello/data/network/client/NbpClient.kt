package com.portfello.data.network.client

import com.portfello.data.network.HttpClientProvider
import com.portfello.data.network.model.NbpExchangeRateResponse
import com.portfello.data.network.model.NbpGoldPrice
import com.portfello.data.network.withRetry
import com.portfello.data.repository.HistoryPoint
import com.portfello.data.repository.HistoryProvider
import com.portfello.data.repository.PriceProvider
import com.portfello.data.repository.Quote
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NbpClient @Inject constructor(
    private val http: HttpClientProvider
) : PriceProvider, HistoryProvider {

    private val moshi = Moshi.Builder().build()
    private val rateAdapter = moshi.adapter(NbpExchangeRateResponse::class.java)
    private val goldListType = Types.newParameterizedType(List::class.java, NbpGoldPrice::class.java)
    private val goldAdapter = moshi.adapter<List<NbpGoldPrice>>(goldListType)
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override suspend fun getPrice(id: String, currency: String): Result<Quote> = runCatching {
        if (id == "XAU" && currency == "PLN") return@runCatching getGoldPricePln()
        withRetry { fetchRate(id, currency) }
    }

    override suspend fun getHistory(id: String, currency: String, days: Int): Result<List<HistoryPoint>> = runCatching {
        if (id == "XAU" && currency == "PLN") return@runCatching getGoldHistory(days)
        val end = LocalDate.now()
        val start = end.minusDays(days.toLong())
        val url = "https://api.nbp.pl/api/exchangerates/rates/A/$id/${start.format(fmt)}/${end.format(fmt)}/?format=json"
        val json = withRetry { get(url) }
        val resp = rateAdapter.fromJson(json) ?: throw Exception("NBP parse error")
        resp.rates.map { rate ->
            HistoryPoint(
                timestamp = LocalDate.parse(rate.effectiveDate).atStartOfDay(ZoneId.of("Europe/Warsaw")).toInstant().toEpochMilli(),
                price = rate.mid,
                currency = "PLN"
            )
        }
    }

    suspend fun getGoldPricePln(): Quote {
        val url = "https://api.nbp.pl/api/cenyzlota/?format=json"
        val json = withRetry { get(url) }
        val prices = goldAdapter.fromJson(json) ?: throw Exception("NBP gold parse error")
        val latest = prices.last()
        return Quote(price = latest.price, currency = "PLN", source = "NBP")
    }

    private suspend fun getGoldHistory(days: Int): List<HistoryPoint> {
        val end = LocalDate.now()
        val start = end.minusDays(days.toLong())
        val url = "https://api.nbp.pl/api/cenyzlota/${start.format(fmt)}/${end.format(fmt)}/?format=json"
        val json = withRetry { get(url) }
        val prices = goldAdapter.fromJson(json) ?: throw Exception("NBP gold parse error")
        return prices.map { p ->
            HistoryPoint(
                timestamp = LocalDate.parse(p.date).atStartOfDay(ZoneId.of("Europe/Warsaw")).toInstant().toEpochMilli(),
                price = p.price,
                currency = "PLN"
            )
        }
    }

    private suspend fun fetchRate(currencyCode: String, targetCurrency: String): Quote {
        // NBP always gives rates in PLN
        val table = if (currencyCode in TABLE_B_CURRENCIES) "B" else "A"
        val url = "https://api.nbp.pl/api/exchangerates/rates/$table/$currencyCode/?format=json"
        val json = get(url)
        val resp = rateAdapter.fromJson(json) ?: throw Exception("NBP parse error")
        val rate = resp.rates.last()
        return Quote(price = rate.mid, currency = "PLN", source = "NBP")
    }

    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        val response = http.client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("NBP HTTP ${response.code}")
        response.body?.string() ?: throw Exception("NBP empty body")
    }

    companion object {
        // Currencies only in NBP Table B (published weekly)
        val TABLE_B_CURRENCIES = setOf(
            "AFN", "MGA", "PAB", "ETB", "VES", "BOB", "ERN", "SZL",
            "GEL", "BWP", "BIF", "GMD", "GHS", "GNF", "HTG", "XOF",
            "XAF", "IQD", "IRR", "YER", "KHR", "QAR", "KES", "LAK",
            "LSL", "LRD", "LYD", "MWK", "MVR", "MRU", "MUR", "MZN",
            "MMK", "NAD", "NPR", "NGN", "NIO", "OMR", "PKR", "PYG",
            "PEN", "RWF", "WST", "SLL", "SCR", "SOS", "SDG", "SRD",
            "TJS", "TZS", "TOP", "TTD", "UGX", "UZS", "VUV", "ZMW",
            "ZWL", "BHD", "JOD", "KWD"
        )
    }
}
