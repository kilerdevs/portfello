package com.portfello.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.portfello.data.AppPrefs
import com.portfello.data.db.dao.PortfolioSnapshotDao
import com.portfello.data.db.entity.PortfolioSnapshot
import com.portfello.data.repository.AssetRepository
import com.portfello.domain.ValuationEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val assetRepo: AssetRepository,
    private val valuationEngine: ValuationEngine,
    private val snapshotDao: PortfolioSnapshotDao,
    private val prefs: AppPrefs
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val assets = assetRepo.getAllAssets().first()
            val baseCurrency = prefs.baseCurrency
            var total = 0.0
            assets.forEach { asset ->
                val v = valuationEngine.valuate(asset, baseCurrency)
                total += v.totalValue
            }
            if (total > 0) {
                snapshotDao.insert(PortfolioSnapshot(
                    timestamp = System.currentTimeMillis(),
                    totalValue = total,
                    baseCurrency = baseCurrency
                ))
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
