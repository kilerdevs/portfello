package com.portfello.data.repository

import com.portfello.data.db.entity.AssetType
import com.portfello.data.network.PriceCache
import com.portfello.data.network.client.CoinGeckoClient
import com.portfello.data.network.client.FrankfurterClient
import com.portfello.data.network.client.NbpClient
import com.portfello.data.network.client.YahooFinanceClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PriceRepository @Inject constructor(
    private val nbp: NbpClient,
    private val yahoo: YahooFinanceClient,
    private val coinGecko: CoinGeckoClient,
    private val frankfurter: FrankfurterClient,
    private val cache: PriceCache
) {
    suspend fun getPrice(assetType: AssetType, tickerOrId: String, currency: String = "PLN"): Result<Quote> {
        val cacheKey = cache.key(assetType.name, tickerOrId, currency)
        cache.get(cacheKey)?.let { return Result.success(it) }

        val result = when (assetType) {
            AssetType.STOCK, AssetType.BOND_TRADED -> yahoo.getPrice(tickerOrId, currency)
            AssetType.CURRENCY -> getCurrencyRate(tickerOrId, currency)
            AssetType.CRYPTO -> coinGecko.getPrice(tickerOrId, currency)
            AssetType.METAL_BULLION -> getMetalSpot(tickerOrId, currency)
            AssetType.BOND_RETAIL, AssetType.MANUAL -> Result.failure(Exception("No auto-pricing for $assetType"))
        }

        result.onSuccess { cache.put(cacheKey, it) }
        return result
    }

    suspend fun getHistory(assetType: AssetType, tickerOrId: String, currency: String, days: Int): Result<List<HistoryPoint>> {
        return when (assetType) {
            AssetType.STOCK, AssetType.BOND_TRADED -> yahoo.getHistory(tickerOrId, currency, days)
            AssetType.CURRENCY -> nbp.getHistory(tickerOrId, currency, days)
                .recoverCatching { frankfurter.getHistory(tickerOrId, currency, days).getOrThrow() }
            AssetType.CRYPTO -> coinGecko.getHistory(tickerOrId, currency, days)
            AssetType.METAL_BULLION -> getMetalHistory(tickerOrId, days)
            AssetType.BOND_RETAIL, AssetType.MANUAL -> Result.failure(Exception("No history for $assetType"))
        }
    }

    suspend fun search(assetType: AssetType, query: String): Result<List<SearchResult>> {
        return when (assetType) {
            AssetType.STOCK, AssetType.BOND_TRADED -> yahoo.search(query)
            AssetType.CRYPTO -> coinGecko.search(query)
            else -> Result.success(emptyList())
        }
    }

    suspend fun getUsdPlnRate(): Double {
        return nbp.getPrice("USD", "PLN").getOrNull()?.price
            ?: frankfurter.getPrice("USD", "PLN").getOrNull()?.price
            ?: throw Exception("Cannot fetch USD/PLN rate")
    }

    private suspend fun getCurrencyRate(code: String, target: String): Result<Quote> {
        return nbp.getPrice(code, target)
            .recoverCatching { frankfurter.getPrice(code, target).getOrThrow() }
    }

    private suspend fun getMetalSpot(metal: String, currency: String): Result<Quote> {
        if (metal == "Au" && currency == "PLN") {
            return runCatching { nbp.getGoldPricePln() }
        }
        val ticker = metalToYahooTicker(metal)
        return yahoo.getPrice(ticker, "USD")
    }

    private suspend fun getMetalHistory(metal: String, days: Int): Result<List<HistoryPoint>> {
        if (metal == "Au") return nbp.getHistory("XAU", "PLN", days)
        val ticker = metalToYahooTicker(metal)
        return yahoo.getHistory(ticker, "USD", days)
    }

    private fun metalToYahooTicker(metal: String): String = when (metal) {
        "Au" -> "GC=F"
        "Ag" -> YahooFinanceClient.SILVER_FUTURES
        "Pt" -> YahooFinanceClient.PLATINUM_FUTURES
        "Pd" -> YahooFinanceClient.PALLADIUM_FUTURES
        else -> throw Exception("Unknown metal: $metal")
    }
}
