package com.portfello.data.crypto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CryptoConfig(
    val salt: String,       // base64-encoded
    val argonTimeCost: Int = 3,
    val argonMemoryCost: Int = 65536, // 64 MB
    val argonParallelism: Int = 2,
    val pinHash: String     // base64 of Argon2id hash — for PIN verification only
)
