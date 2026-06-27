package com.portfello.data.repository

data class Quote(
    val price: Double,
    val currency: String,
    val source: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class HistoryPoint(
    val timestamp: Long,
    val price: Double,
    val currency: String
)

data class SearchResult(
    val id: String,
    val name: String,
    val symbol: String,
    val exchange: String? = null
)

interface PriceProvider {
    suspend fun getPrice(id: String, currency: String = "PLN"): Result<Quote>
}

interface HistoryProvider {
    suspend fun getHistory(id: String, currency: String, days: Int): Result<List<HistoryPoint>>
}

interface SearchProvider {
    suspend fun search(query: String): Result<List<SearchResult>>
}
