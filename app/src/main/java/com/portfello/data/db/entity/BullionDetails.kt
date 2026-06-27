package com.portfello.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "bullion_details",
    foreignKeys = [ForeignKey(
        entity = Asset::class,
        parentColumns = ["id"],
        childColumns = ["asset_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class BullionDetails(
    @PrimaryKey @ColumnInfo(name = "asset_id") val assetId: Long,
    val metal: String, // Au, Ag, Pt, Pd
    @ColumnInfo(name = "total_weight_grams") val totalWeightGrams: Double,
    val purity: Double,
    @ColumnInfo(name = "premium_pln") val premiumPln: Double = 0.0
)
