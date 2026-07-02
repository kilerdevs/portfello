package com.portfello

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.portfello.data.AppPrefs
import com.portfello.data.crypto.LockState
import com.portfello.data.network.client.CoinGeckoClient
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PortfelloApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var prefs: AppPrefs

    @Inject
    lateinit var coinGeckoClient: CoinGeckoClient

    @Inject
    lateinit var lockState: LockState

    override fun onCreate() {
        super.onCreate()
        coinGeckoClient.apiKey = prefs.coinGeckoApiKey.ifBlank { null }
        lockState.lockTimeoutMs = prefs.lockTimeoutSec * 1000
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
