package com.portfello.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "asset_holding",
    foreignKeys = [ForeignKey(
        entity = Asset::class,
        parentColumns = ["id"],
        childColumns = ["asset_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("asset_id")]
)
data class AssetHolding(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "asset_id") val assetId: Long,
    val quantity: Double,
    @ColumnInfo(name = "purchase_price") val purchasePrice: Double? = null,
    @ColumnInfo(name = "purchase_date") val purchaseDate: Long? = null
)
