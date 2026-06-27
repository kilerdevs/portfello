package com.portfello.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.portfello.data.db.entity.BondRetailDetails

@Dao
interface BondRetailDetailsDao {
    @Query("SELECT * FROM bond_retail_details WHERE asset_id = :assetId")
    suspend fun getByAssetId(assetId: Long): BondRetailDetails?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(details: BondRetailDetails)
}
