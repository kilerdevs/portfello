package com.portfello.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "asset")
data class Asset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: AssetType,
    val name: String,
    @ColumnInfo(name = "ticker_or_id") val tickerOrId: String? = null,
    val currency: String = "PLN",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    val notes: String? = null
)
