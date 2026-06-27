package com.portfello.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.portfello.data.db.entity.BullionDetails

@Dao
interface BullionDetailsDao {
    @Query("SELECT * FROM bullion_details WHERE asset_id = :assetId")
    suspend fun getByAssetId(assetId: Long): BullionDetails?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(details: BullionDetails)
}
