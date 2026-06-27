package com.portfello.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "portfolio_snapshot")
data class PortfolioSnapshot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    @ColumnInfo(name = "total_value") val totalValue: Double,
    @ColumnInfo(name = "base_currency") val baseCurrency: String
)
