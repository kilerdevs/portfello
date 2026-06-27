package com.portfello.di

import android.content.Context
import androidx.room.Room
import com.portfello.data.crypto.KeyManager
import com.portfello.data.db.PortfelloDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        keyManager: KeyManager
    ): PortfelloDatabase {
        System.loadLibrary("sqlcipher")
        val key = keyManager.getDatabaseKey()
        val factory = SupportOpenHelperFactory(key, null, false)
        return Room.databaseBuilder(
            context,
            PortfelloDatabase::class.java,
            "portfello.db"
        )
            .openHelperFactory(factory)
            .build()
    }

    @Provides fun assetDao(db: PortfelloDatabase) = db.assetDao()
    @Provides fun assetHoldingDao(db: PortfelloDatabase) = db.assetHoldingDao()
    @Provides fun priceSnapshotDao(db: PortfelloDatabase) = db.priceSnapshotDao()
    @Provides fun portfolioSnapshotDao(db: PortfelloDatabase) = db.portfolioSnapshotDao()
    @Provides fun bondRetailDetailsDao(db: PortfelloDatabase) = db.bondRetailDetailsDao()
    @Provides fun bullionDetailsDao(db: PortfelloDatabase) = db.bullionDetailsDao()
}
