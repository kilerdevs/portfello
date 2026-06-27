package com.portfello.data.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CoinGeckoSearchResponse(
    val coins: List<CoinGeckoSearchCoin>
)

@JsonClass(generateAdapter = true)
data class CoinGeckoSearchCoin(
    val id: String,
    val name: String,
    val symbol: String,
    @Json(name = "market_cap_rank") val marketCapRank: Int?
)

@JsonClass(generateAdapter = true)
data class CoinGeckoMarketChart(
    val prices: List<List<Double>>
)
