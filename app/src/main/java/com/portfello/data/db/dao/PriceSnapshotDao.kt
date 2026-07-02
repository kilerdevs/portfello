package com.portfello.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.portfello.data.db.entity.PriceSnapshot
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceSnapshotDao {
    @Query("SELECT * FROM price_snapshot WHERE asset_id = :assetId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(assetId: Long): PriceSnapshot?

    @Query("SELECT * FROM price_snapshot WHERE asset_id = :assetId AND timestamp <= :ts ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestBefore(assetId: Long, ts: Long): PriceSnapshot?

    @Query("SELECT * FROM price_snapshot WHERE asset_id = :assetId AND timestamp >= :since ORDER BY timestamp")
    fun getHistory(assetId: Long, since: Long): Flow<List<PriceSnapshot>>

    @Insert
    suspend fun insert(snapshot: PriceSnapshot)

    @Insert
    suspend fun insertAll(snapshots: List<PriceSnapshot>)
}
