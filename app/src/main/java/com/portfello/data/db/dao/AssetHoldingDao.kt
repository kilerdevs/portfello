package com.portfello.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.portfello.data.db.entity.AssetHolding
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetHoldingDao {
    @Query("SELECT * FROM asset_holding WHERE asset_id = :assetId")
    fun getHoldingsForAsset(assetId: Long): Flow<List<AssetHolding>>

    @Insert
    suspend fun insert(holding: AssetHolding): Long

    @Update
    suspend fun update(holding: AssetHolding)

    @Delete
    suspend fun delete(holding: AssetHolding)
}
