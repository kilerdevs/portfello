package com.portfello.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "price_snapshot",
    foreignKeys = [ForeignKey(
        entity = Asset::class,
        parentColumns = ["id"],
        childColumns = ["asset_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("asset_id")]
)
data class PriceSnapshot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "asset_id") val assetId: Long,
    val timestamp: Long,
    val price: Double,
    val currency: String,
    val source: String
)
