package com.portfello.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.portfello.data.db.entity.PortfolioSnapshot
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioSnapshotDao {
    @Query("SELECT * FROM portfolio_snapshot WHERE timestamp >= :since ORDER BY timestamp")
    fun getHistory(since: Long): Flow<List<PortfolioSnapshot>>

    @Query("SELECT * FROM portfolio_snapshot WHERE timestamp >= :since AND base_currency = :baseCurrency ORDER BY timestamp")
    suspend fun getHistoryList(since: Long, baseCurrency: String): List<PortfolioSnapshot>

    @Query("SELECT * FROM portfolio_snapshot WHERE base_currency = :baseCurrency ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(baseCurrency: String): PortfolioSnapshot?

    @Insert
    suspend fun insert(snapshot: PortfolioSnapshot)
}
