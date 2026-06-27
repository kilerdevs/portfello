package com.portfello.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.portfello.data.db.entity.Asset
import com.portfello.data.db.entity.AssetType
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetDao {
    @Query("SELECT * FROM asset ORDER BY type, name")
    fun getAllAssets(): Flow<List<Asset>>

    @Query("SELECT * FROM asset WHERE type = :type ORDER BY name")
    fun getAssetsByType(type: AssetType): Flow<List<Asset>>

    @Query("SELECT * FROM asset WHERE id = :id")
    suspend fun getById(id: Long): Asset?

    @Insert
    suspend fun insert(asset: Asset): Long

    @Update
    suspend fun update(asset: Asset)

    @Delete
    suspend fun delete(asset: Asset)
}
