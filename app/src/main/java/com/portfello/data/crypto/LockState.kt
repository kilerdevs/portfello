package com.portfello.data.crypto

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class AppLockStatus {
    NEEDS_SETUP,
    LOCKED,
    UNLOCKED
}

@Singleton
class LockState @Inject constructor(
    private val keyManager: KeyManager
) {
    private val _status = MutableStateFlow(
        if (keyManager.isSetUp) AppLockStatus.LOCKED else AppLockStatus.NEEDS_SETUP
    )
    val status: StateFlow<AppLockStatus> = _status

    private var lastActiveTime = 0L
    var lockTimeoutMs = 60_000L // default 1 min, configurable

    fun onUnlocked() {
        _status.value = AppLockStatus.UNLOCKED
        lastActiveTime = System.currentTimeMillis()
    }

    fun onPinSetup() {
        _status.value = AppLockStatus.UNLOCKED
        lastActiveTime = System.currentTimeMillis()
    }

    fun onAppBackground() {
        lastActiveTime = System.currentTimeMillis()
    }

    fun onAppForeground() {
        if (_status.value == AppLockStatus.UNLOCKED) {
            val elapsed = System.currentTimeMillis() - lastActiveTime
            if (elapsed > lockTimeoutMs) {
                keyManager.lock()
                _status.value = AppLockStatus.LOCKED
            }
        }
    }

    fun lock() {
        keyManager.lock()
        _status.value = AppLockStatus.LOCKED
    }

    fun resetToSetup() {
        keyManager.lock()
        _status.value = AppLockStatus.NEEDS_SETUP
    }
}
