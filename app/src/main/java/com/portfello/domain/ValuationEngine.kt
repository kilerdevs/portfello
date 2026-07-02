package com.portfello.domain

import com.portfello.data.db.dao.AssetHoldingDao
import com.portfello.data.db.dao.BondRetailDetailsDao
import com.portfello.data.db.dao.BullionDetailsDao
import com.portfello.data.db.dao.PriceSnapshotDao
import com.portfello.data.db.entity.Asset
import com.portfello.data.db.entity.AssetType
import com.portfello.data.db.entity.PriceSnapshot
import com.portfello.data.repository.PriceRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

data class AssetValuation(
    val asset: Asset,
    val totalValue: Double,
    val baseCurrency: String,
    val pricePerUnit: Double?,
    val priceCurrency: String?,
    val lastUpdated: Long?,
    val error: String? = null,
    val costBasisInBase: Double? = null
) {
    val profitLoss: Double?
        get() = costBasisInBase?.let { totalValue - it }

    val profitLossPct: Double?
        get() = costBasisInBase?.takeIf { it != 0.0 }?.let { (totalValue - it) / it * 100 }
}

@Singleton
class ValuationEngine @Inject constructor(
    private val priceRepo: PriceRepository,
    private val holdingDao: AssetHoldingDao,
    private val bondDao: BondRetailDetailsDao,
    private val bullionDao: BullionDetailsDao,
    private val priceSnapshotDao: PriceSnapshotDao,
    private val bullionValuator: BullionValuator,
    private val currencyConverter: CurrencyConverter
) {
    suspend fun valuate(asset: Asset, baseCurrency: String): AssetValuation {
        val result = try {
            when (asset.type) {
                AssetType.STOCK, AssetType.BOND_TRADED -> valuateMarketAsset(asset, baseCurrency)
                AssetType.CURRENCY -> valuateCurrency(asset, baseCurrency)
                AssetType.CRYPTO -> valuateCrypto(asset, baseCurrency)
                AssetType.METAL_BULLION -> valuateBullion(asset, baseCurrency)
                AssetType.BOND_RETAIL -> valuateBondRetail(asset, baseCurrency)
                AssetType.MANUAL -> valuateManual(asset, baseCurrency)
            }
        } catch (e: Exception) {
            fallbackValuation(asset, baseCurrency, e.message ?: "Unknown error")
        }
        return result.copy(costBasisInBase = costBasis(asset, baseCurrency))
    }

    // ponytail: cost basis converted at the current FX rate; historical FX if anyone asks
    private suspend fun costBasis(asset: Asset, baseCurrency: String): Double? {
        val priced = holdingDao.getHoldingsForAsset(asset.id).first().filter { it.purchasePrice != null }
        if (priced.isEmpty()) return null
        val costInAssetCur = priced.sumOf { it.quantity * it.purchasePrice!! }
        return try {
            currencyConverter.convert(costInAssetCur, asset.currency, baseCurrency)
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun valuateMarketAsset(asset: Asset, baseCurrency: String): AssetValuation {
        val ticker = asset.tickerOrId ?: throw Exception("No ticker")
        val quote = priceRepo.getPrice(asset.type, ticker, asset.currency).getOrThrow()
        saveSnapshot(asset, quote.price, quote.currency)
        val holdings = holdingDao.getHoldingsForAsset(asset.id).first()
        val totalQty = holdings.sumOf { it.quantity }
        val valueInAssetCur = totalQty * quote.price
        val valueInBase = currencyConverter.convert(valueInAssetCur, quote.currency, baseCurrency)
        return AssetValuation(asset, valueInBase, baseCurrency, quote.price, quote.currency, quote.timestamp)
    }

    private suspend fun valuateCurrency(asset: Asset, baseCurrency: String): AssetValuation {
        val holdings = holdingDao.getHoldingsForAsset(asset.id).first()
        val totalAmount = holdings.sumOf { it.quantity }
        val code = asset.tickerOrId ?: asset.currency
        val valueInBase = currencyConverter.convert(totalAmount, code, baseCurrency)
        val rate = currencyConverter.getRate(code, baseCurrency)
        return AssetValuation(asset, valueInBase, baseCurrency, rate, baseCurrency, System.currentTimeMillis())
    }

    private suspend fun valuateCrypto(asset: Asset, baseCurrency: String): AssetValuation {
        val coinId = asset.tickerOrId ?: throw Exception("No CoinGecko ID")
        val quote = priceRepo.getPrice(AssetType.CRYPTO, coinId, baseCurrency).getOrThrow()
        saveSnapshot(asset, quote.price, quote.currency)
        val holdings = holdingDao.getHoldingsForAsset(asset.id).first()
        val totalQty = holdings.sumOf { it.quantity }
        return AssetValuation(asset, totalQty * quote.price, baseCurrency, quote.price, quote.currency, quote.timestamp)
    }

    private suspend fun valuateBullion(asset: Asset, baseCurrency: String): AssetValuation {
        val details = bullionDao.getByAssetId(asset.id) ?: throw Exception("No bullion details")
        val valuePerUnit = bullionValuator.valuePerUnit(details)
        val holdings = holdingDao.getHoldingsForAsset(asset.id).first()
        val totalUnits = holdings.sumOf { it.quantity }
        val totalPln = totalUnits * valuePerUnit
        val valueInBase = currencyConverter.convert(totalPln, "PLN", baseCurrency)
        return AssetValuation(asset, valueInBase, baseCurrency, valuePerUnit, "PLN", System.currentTimeMillis())
    }

    private suspend fun valuateBondRetail(asset: Asset, baseCurrency: String): AssetValuation {
        val details = bondDao.getByAssetId(asset.id) ?: throw Exception("No bond details")
        val holdings = holdingDao.getHoldingsForAsset(asset.id).first()
        val now = System.currentTimeMillis()
        var totalValue = 0.0
        for (h in holdings) {
            val purchaseDate = h.purchaseDate ?: asset.createdAt
            val unitValue = BondRetailCalculator.estimatedValue(details, purchaseDate, now)
            totalValue += h.quantity * unitValue
        }
        val valueInBase = currencyConverter.convert(totalValue, "PLN", baseCurrency)
        return AssetValuation(asset, valueInBase, baseCurrency, null, "PLN", now)
    }

    private suspend fun valuateManual(asset: Asset, baseCurrency: String): AssetValuation {
        val snapshot = priceSnapshotDao.getLatest(asset.id)
        val holdings = holdingDao.getHoldingsForAsset(asset.id).first()
        val totalQty = holdings.sumOf { it.quantity }
        val price = snapshot?.price ?: 0.0
        val priceCurrency = snapshot?.currency ?: asset.currency
        val valueInBase = currencyConverter.convert(totalQty * price, priceCurrency, baseCurrency)
        return AssetValuation(asset, valueInBase, baseCurrency, price, priceCurrency, snapshot?.timestamp)
    }

    private suspend fun fallbackValuation(asset: Asset, baseCurrency: String, error: String): AssetValuation {
        val snapshot = priceSnapshotDao.getLatest(asset.id)
        val holdings = holdingDao.getHoldingsForAsset(asset.id).first()
        val totalQty = holdings.sumOf { it.quantity }
        val value = if (snapshot != null) {
            currencyConverter.convert(totalQty * snapshot.price, snapshot.currency, baseCurrency)
        } else 0.0
        return AssetValuation(asset, value, baseCurrency, snapshot?.price, snapshot?.currency, snapshot?.timestamp, error)
    }

    private suspend fun saveSnapshot(asset: Asset, price: Double, currency: String) {
        priceSnapshotDao.insert(
            PriceSnapshot(
                assetId = asset.id,
                timestamp = System.currentTimeMillis(),
                price = price,
                currency = currency,
                source = "auto"
            )
        )
    }
}
