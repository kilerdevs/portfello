package com.portfello.data.network.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FrankfurterResponse(
    val base: String,
    val date: String,
    val rates: Map<String, Double>
)
