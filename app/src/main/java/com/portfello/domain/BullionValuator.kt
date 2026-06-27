package com.portfello.domain

import com.portfello.data.db.entity.BullionDetails
import com.portfello.data.repository.PriceRepository
import javax.inject.Inject
import javax.inject.Singleton

const val TROY_OZ_GRAMS = 31.1034768

@Singleton
class BullionValuator @Inject constructor(
    private val priceRepo: PriceRepository
) {
    suspend fun valuePerUnit(details: BullionDetails): Double {
        val spotPerGramPln = when (details.metal) {
            "Au" -> {
                val goldPln = priceRepo.getPrice(
                    com.portfello.data.db.entity.AssetType.METAL_BULLION, "Au", "PLN"
                ).getOrThrow()
                goldPln.price
            }
            else -> {
                val usdPerOz = priceRepo.getPrice(
                    com.portfello.data.db.entity.AssetType.METAL_BULLION, details.metal, "USD"
                ).getOrThrow().price
                val usdPln = priceRepo.getUsdPlnRate()
                (usdPerOz / TROY_OZ_GRAMS) * usdPln
            }
        }
        val pureWeightGrams = details.totalWeightGrams * details.purity
        return pureWeightGrams * spotPerGramPln + details.premiumPln
    }
}
