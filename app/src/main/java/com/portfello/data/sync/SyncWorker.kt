package com.portfello.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.portfello.data.AppPrefs
import com.portfello.data.crypto.KeyManager
import com.portfello.data.db.dao.PortfolioSnapshotDao
import com.portfello.data.db.entity.PortfolioSnapshot
import com.portfello.data.repository.AssetRepository
import com.portfello.domain.ValuationEngine
import dagger.Lazy
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val keyManager: KeyManager,
    private val assetRepo: Lazy<AssetRepository>,
    private val valuationEngine: Lazy<ValuationEngine>,
    private val snapshotDao: Lazy<PortfolioSnapshotDao>,
    private val prefs: AppPrefs
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // DB key lives only in memory after unlock — skip quietly when locked
        if (!keyManager.isUnlocked) return Result.success()
        return try {
            val assets = assetRepo.get().getAllAssets().first()
            val baseCurrency = prefs.baseCurrency
            var total = 0.0
            assets.forEach { asset ->
                val v = valuationEngine.get().valuate(asset, baseCurrency)
                total += v.totalValue
            }
            if (total > 0) {
                snapshotDao.get().insert(PortfolioSnapshot(
                    timestamp = System.currentTimeMillis(),
                    totalValue = total,
                    baseCurrency = baseCurrency
                ))
            }
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            if (runAttemptCount >= 3) Result.failure() else Result.retry()
        }
    }
}
