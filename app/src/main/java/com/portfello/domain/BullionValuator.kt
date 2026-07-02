package com.portfello.domain

import com.portfello.data.db.entity.AssetType
import com.portfello.data.db.entity.BullionDetails
import com.portfello.data.repository.HistoryPoint
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

    /** Per-unit value history in PLN: spot history scaled by weight x purity, plus premium. */
    suspend fun unitValueHistory(details: BullionDetails, days: Int): Result<List<HistoryPoint>> = runCatching {
        val pureWeightGrams = details.totalWeightGrams * details.purity
        when (details.metal) {
            // NBP gold history is PLN per gram
            "Au" -> priceRepo.getHistory(AssetType.METAL_BULLION, "Au", "PLN", days).getOrThrow()
                .map { it.copy(price = it.price * pureWeightGrams + details.premiumPln, currency = "PLN") }
            // Yahoo futures history is USD per troy oz
            else -> {
                // ponytail: today's USD/PLN applied to the whole series; historical FX if anyone asks
                val usdPln = priceRepo.getUsdPlnRate()
                priceRepo.getHistory(AssetType.METAL_BULLION, details.metal, "USD", days).getOrThrow()
                    .map {
                        it.copy(
                            price = (it.price / TROY_OZ_GRAMS) * usdPln * pureWeightGrams + details.premiumPln,
                            currency = "PLN"
                        )
                    }
            }
        }
    }
}
