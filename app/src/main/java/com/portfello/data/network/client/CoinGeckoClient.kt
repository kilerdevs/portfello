package com.portfello.data.network.client

import com.portfello.data.network.HttpClientProvider
import com.portfello.data.network.model.CoinGeckoMarketChart
import com.portfello.data.network.model.CoinGeckoSearchResponse
import com.portfello.data.network.withRetry
import com.portfello.data.repository.HistoryPoint
import com.portfello.data.repository.HistoryProvider
import com.portfello.data.repository.PriceProvider
import com.portfello.data.repository.Quote
import com.portfello.data.repository.SearchProvider
import com.portfello.data.repository.SearchResult
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoinGeckoClient @Inject constructor(
    private val http: HttpClientProvider
) : PriceProvider, HistoryProvider, SearchProvider {

    private val moshi = Moshi.Builder().build()
    private val searchAdapter = moshi.adapter(CoinGeckoSearchResponse::class.java)
    private val chartAdapter = moshi.adapter(CoinGeckoMarketChart::class.java)
    private val mapType = Types.newParameterizedType(
        Map::class.java, String::class.java,
        Types.newParameterizedType(Map::class.java, String::class.java, Double::class.javaObjectType)
    )
    private val priceAdapter = moshi.adapter<Map<String, Map<String, Double>>>(mapType)

    @Volatile
    var apiKey: String? = null

    override suspend fun getPrice(id: String, currency: String): Result<Quote> = runCatching {
        val cur = currency.lowercase()
        val url = "$BASE/simple/price?ids=$id&vs_currencies=$cur"
        val json = withRetry { get(url) }
        val map = priceAdapter.fromJson(json) ?: throw Exception("CoinGecko parse error")
        val price = map[id]?.get(cur) ?: throw Exception("CoinGecko: no price for $id/$cur")
        Quote(price = price, currency = currency.uppercase(), source = "CoinGecko")
    }

    override suspend fun getHistory(id: String, currency: String, days: Int): Result<List<HistoryPoint>> = runCatching {
        val cur = currency.lowercase()
        val url = "$BASE/coins/$id/market_chart?vs_currency=$cur&days=$days"
        val json = withRetry { get(url) }
        val chart = chartAdapter.fromJson(json) ?: throw Exception("CoinGecko parse error")
        chart.prices.map { point ->
            HistoryPoint(
                timestamp = point[0].toLong(),
                price = point[1],
                currency = currency.uppercase()
            )
        }
    }

    override suspend fun search(query: String): Result<List<SearchResult>> = runCatching {
        val url = "$BASE/search?query=${java.net.URLEncoder.encode(query, "UTF-8")}"
        val json = withRetry { get(url) }
        val resp = searchAdapter.fromJson(json) ?: throw Exception("CoinGecko parse error")
        resp.coins.take(20).map { coin ->
            SearchResult(
                id = coin.id,
                name = coin.name,
                symbol = coin.symbol.uppercase(),
                exchange = "CoinGecko"
            )
        }
    }

    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val builder = Request.Builder().url(url)
        apiKey?.let { builder.header("x-cg-demo-api-key", it) }
        val response = http.client.newCall(builder.build()).execute()
        if (!response.isSuccessful) throw Exception("CoinGecko HTTP ${response.code}")
        response.body?.string() ?: throw Exception("CoinGecko empty body")
    }

    companion object {
        private const val BASE = "https://api.coingecko.com/api/v3"
    }
}
