package com.portfello.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "bond_retail_details",
    foreignKeys = [ForeignKey(
        entity = Asset::class,
        parentColumns = ["id"],
        childColumns = ["asset_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class BondRetailDetails(
    @PrimaryKey @ColumnInfo(name = "asset_id") val assetId: Long,
    val nominal: Double,
    val series: String,
    @ColumnInfo(name = "interest_rate") val interestRate: Double,
    @ColumnInfo(name = "rate_updated_at") val rateUpdatedAt: Long,
    @ColumnInfo(name = "capitalization_type") val capitalizationType: String // "ANNUAL" or "END"
)
