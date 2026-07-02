package com.portfello.domain

import com.portfello.data.repository.PriceRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrencyConverter @Inject constructor(
    private val priceRepo: PriceRepository
) {
    private data class Entry(val rate: Double, val expiresAt: Long)

    // ponytail: last write wins on concurrent fetches — both computed the same rate anyway
    private val rateCache = java.util.concurrent.ConcurrentHashMap<String, Entry>()
    var ttlMs: Long = 15 * 60_000L

    suspend fun convert(amount: Double, from: String, to: String): Double {
        if (from == to) return amount
        val rate = getRate(from, to)
        return amount * rate
    }

    suspend fun getRate(from: String, to: String): Double {
        if (from == to) return 1.0
        val key = "$from->$to"
        rateCache[key]?.let {
            if (System.currentTimeMillis() < it.expiresAt) return it.rate
            rateCache.remove(key)
        }

        val rate = if (to == "PLN") {
            priceRepo.getPrice(
                com.portfello.data.db.entity.AssetType.CURRENCY, from, "PLN"
            ).getOrNull()?.price ?: throw Exception("No rate $from/$to")
        } else if (from == "PLN") {
            val inversRate = getRate(to, "PLN")
            1.0 / inversRate
        } else {
            val fromPln = getRate(from, "PLN")
            val toPln = getRate(to, "PLN")
            fromPln / toPln
        }
        rateCache[key] = Entry(rate, System.currentTimeMillis() + ttlMs)
        return rate
    }

    fun clearCache() = rateCache.clear()
}
