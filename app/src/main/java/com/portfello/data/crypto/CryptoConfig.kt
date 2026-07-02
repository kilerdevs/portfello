package com.portfello.data.crypto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CryptoConfig(
    val salt: String,       // base64-encoded
    val argonTimeCost: Int = 3,
    val argonMemoryCost: Int = 65536, // 64 MB
    val argonParallelism: Int = 2,
    // v1: base64 of the raw Argon2id hash (== DB key, plaintext on disk — legacy)
    // v2: base64 of SHA-256(DB key) — verification only, key never stored
    val pinHash: String,
    val version: Int = 1
)
