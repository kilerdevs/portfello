package com.portfello.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.portfello.data.db.converter.Converters
import com.portfello.data.db.dao.AssetDao
import com.portfello.data.db.dao.AssetHoldingDao
import com.portfello.data.db.dao.BondRetailDetailsDao
import com.portfello.data.db.dao.BullionDetailsDao
import com.portfello.data.db.dao.PortfolioSnapshotDao
import com.portfello.data.db.dao.PriceSnapshotDao
import com.portfello.data.db.entity.Asset
import com.portfello.data.db.entity.AssetHolding
import com.portfello.data.db.entity.BondRetailDetails
import com.portfello.data.db.entity.BullionDetails
import com.portfello.data.db.entity.PortfolioSnapshot
import com.portfello.data.db.entity.PriceSnapshot

@Database(
    entities = [
        Asset::class,
        AssetHolding::class,
        PriceSnapshot::class,
        PortfolioSnapshot::class,
        BondRetailDetails::class,
        BullionDetails::class,
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PortfelloDatabase : RoomDatabase() {
    abstract fun assetDao(): AssetDao
    abstract fun assetHoldingDao(): AssetHoldingDao
    abstract fun priceSnapshotDao(): PriceSnapshotDao
    abstract fun portfolioSnapshotDao(): PortfolioSnapshotDao
    abstract fun bondRetailDetailsDao(): BondRetailDetailsDao
    abstract fun bullionDetailsDao(): BullionDetailsDao
}
