package com.portfello.data.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NbpExchangeRateResponse(
    val table: String,
    val currency: String,
    val code: String,
    val rates: List<NbpRate>
)

@JsonClass(generateAdapter = true)
data class NbpRate(
    val no: String,
    @Json(name = "effectiveDate") val effectiveDate: String,
    val mid: Double
)

@JsonClass(generateAdapter = true)
data class NbpGoldPrice(
    @Json(name = "data") val date: String,
    @Json(name = "cena") val price: Double
)
