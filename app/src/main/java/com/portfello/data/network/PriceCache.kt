package com.portfello.data.network

import com.portfello.data.repository.Quote
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PriceCache @Inject constructor() {

    private data class Entry(val quote: Quote, val expiresAt: Long)

    private val cache = java.util.concurrent.ConcurrentHashMap<String, Entry>()
    private val defaultTtlMs = 5 * 60 * 1000L // 5 min

    fun get(key: String): Quote? {
        val entry = cache[key] ?: return null
        if (System.currentTimeMillis() > entry.expiresAt) {
            cache.remove(key)
            return null
        }
        return entry.quote
    }

    fun put(key: String, quote: Quote, ttlMs: Long = defaultTtlMs) {
        cache[key] = Entry(quote, System.currentTimeMillis() + ttlMs)
    }

    fun key(source: String, id: String, currency: String) = "$source:$id:$currency"
}
