package com.portfello.data.repository

import com.portfello.data.db.dao.AssetDao
import com.portfello.data.db.dao.AssetHoldingDao
import com.portfello.data.db.dao.BondRetailDetailsDao
import com.portfello.data.db.dao.BullionDetailsDao
import com.portfello.data.db.entity.Asset
import com.portfello.data.db.entity.AssetHolding
import com.portfello.data.db.entity.AssetType
import com.portfello.data.db.entity.BondRetailDetails
import com.portfello.data.db.entity.BullionDetails
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetRepository @Inject constructor(
    private val assetDao: AssetDao,
    private val holdingDao: AssetHoldingDao,
    private val bondDao: BondRetailDetailsDao,
    private val bullionDao: BullionDetailsDao
) {
    fun getAllAssets(): Flow<List<Asset>> = assetDao.getAllAssets()
    fun getAssetsByType(type: AssetType): Flow<List<Asset>> = assetDao.getAssetsByType(type)
    fun getHoldings(assetId: Long): Flow<List<AssetHolding>> = holdingDao.getHoldingsForAsset(assetId)

    suspend fun getById(id: Long): Asset? = assetDao.getById(id)

    suspend fun addAsset(asset: Asset): Long = assetDao.insert(asset)
    suspend fun updateAsset(asset: Asset) = assetDao.update(asset)
    suspend fun deleteAsset(asset: Asset) = assetDao.delete(asset)

    suspend fun addHolding(holding: AssetHolding): Long = holdingDao.insert(holding)
    suspend fun updateHolding(holding: AssetHolding) = holdingDao.update(holding)
    suspend fun deleteHolding(holding: AssetHolding) = holdingDao.delete(holding)

    suspend fun saveBondDetails(details: BondRetailDetails) = bondDao.upsert(details)
    suspend fun getBondDetails(assetId: Long) = bondDao.getByAssetId(assetId)

    suspend fun saveBullionDetails(details: BullionDetails) = bullionDao.upsert(details)
    suspend fun getBullionDetails(assetId: Long) = bullionDao.getByAssetId(assetId)
}
