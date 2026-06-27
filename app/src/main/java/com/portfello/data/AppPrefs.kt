package com.portfello.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPrefs @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("portfello_prefs", Context.MODE_PRIVATE)

    var baseCurrency: String
        get() = prefs.getString("base_currency", "PLN") ?: "PLN"
        set(value) = prefs.edit().putString("base_currency", value).apply()

    var syncIntervalMin: Long
        get() = prefs.getLong("sync_interval_min", 240)
        set(value) = prefs.edit().putLong("sync_interval_min", value).apply()

    var coinGeckoApiKey: String
        get() = prefs.getString("coingecko_key", "") ?: ""
        set(value) = prefs.edit().putString("coingecko_key", value).apply()

    var lockTimeoutSec: Long
        get() = prefs.getLong("lock_timeout_sec", 60)
        set(value) = prefs.edit().putLong("lock_timeout_sec", value).apply()

    // 0 = disabled
    var wipeAfterAttempts: Int
        get() = prefs.getInt("wipe_after_attempts", 0)
        set(value) = prefs.edit().putInt("wipe_after_attempts", value).apply()

    var onboardingComplete: Boolean
        get() = prefs.getBoolean("onboarding_complete", false)
        set(value) = prefs.edit().putBoolean("onboarding_complete", value).apply()
}
