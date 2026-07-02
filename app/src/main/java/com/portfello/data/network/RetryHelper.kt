package com.portfello.data.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

suspend fun <T> withRetry(
    maxAttempts: Int = 3,
    initialDelayMs: Long = 500,
    block: suspend () -> T
): T {
    var lastException: Exception? = null
    repeat(maxAttempts) { attempt ->
        try {
            return block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            lastException = e
            if (attempt < maxAttempts - 1) {
                delay(initialDelayMs * (1L shl attempt))
            }
        }
    }
    throw lastException!!
}
