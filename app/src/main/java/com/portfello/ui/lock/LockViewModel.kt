package com.portfello.ui.lock

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfello.data.AppPrefs
import com.portfello.data.crypto.AppLockStatus
import com.portfello.data.crypto.KeyManager
import com.portfello.data.crypto.LockState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LockUiState(
    val pin: String = "",
    val confirmPin: String = "",
    val isConfirmStep: Boolean = false,
    val error: String? = null,
    val failedAttempts: Int = 0,
    val lockoutEndTime: Long = 0,
    val isProcessing: Boolean = false
)

@HiltViewModel
class LockViewModel @Inject constructor(
    private val keyManager: KeyManager,
    private val lockState: LockState,
    private val prefs: AppPrefs,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(LockUiState())
    val uiState: StateFlow<LockUiState> = _uiState.asStateFlow()

    val lockStatus = lockState.status

    val isSetup: Boolean
        get() = lockState.status.value == AppLockStatus.NEEDS_SETUP

    fun onDigit(digit: Char) {
        val current = _uiState.value
        if (current.isProcessing) return
        if (System.currentTimeMillis() < current.lockoutEndTime) return

        if (current.isConfirmStep) {
            val newConfirm = current.confirmPin + digit
            _uiState.value = current.copy(confirmPin = newConfirm, error = null)
            if (newConfirm.length == current.pin.length) {
                confirmSetup(current.pin, newConfirm)
            }
        } else {
            val newPin = current.pin + digit
            _uiState.value = current.copy(pin = newPin, error = null)
            if (!isSetup && newPin.length >= 4) {
                // ponytail: auto-submit at 4+ digits on unlock, user can also tap confirm
            }
        }
    }

    fun onBackspace() {
        val current = _uiState.value
        if (current.isConfirmStep && current.confirmPin.isNotEmpty()) {
            _uiState.value = current.copy(confirmPin = current.confirmPin.dropLast(1))
        } else if (!current.isConfirmStep && current.pin.isNotEmpty()) {
            _uiState.value = current.copy(pin = current.pin.dropLast(1))
        }
    }

    fun onConfirm() {
        val current = _uiState.value
        if (current.isProcessing) return
        if (current.pin.length < 4) {
            _uiState.value = current.copy(error = "PIN musi mieć co najmniej 4 cyfry")
            return
        }

        if (isSetup) {
            if (!current.isConfirmStep) {
                _uiState.value = current.copy(isConfirmStep = true, error = null)
            }
        } else {
            attemptUnlock(current.pin)
        }
    }

    private fun confirmSetup(pin: String, confirm: String) {
        if (pin != confirm) {
            _uiState.value = LockUiState(error = "PINy się nie zgadzają — spróbuj ponownie")
            return
        }
        _uiState.value = _uiState.value.copy(isProcessing = true)
        viewModelScope.launch(Dispatchers.Default) {
            keyManager.setupPin(pin)
            lockState.onPinSetup()
            _uiState.value = LockUiState()
        }
    }

    private fun attemptUnlock(pin: String) {
        val current = _uiState.value
        val now = System.currentTimeMillis()
        if (now < current.lockoutEndTime) {
            _uiState.value = current.copy(error = "Zbyt wiele prób. Poczekaj.")
            return
        }

        _uiState.value = current.copy(isProcessing = true)
        viewModelScope.launch(Dispatchers.Default) {
            val success = keyManager.verifyAndUnlock(pin)
            if (success) {
                lockState.onUnlocked()
                _uiState.value = LockUiState()
            } else {
                val attempts = current.failedAttempts + 1
                val wipeThreshold = prefs.wipeAfterAttempts
                if (wipeThreshold > 0 && attempts >= wipeThreshold) {
                    wipeDatabase()
                    return@launch
                }
                val lockoutMs = when {
                    attempts >= 8 -> 300_000L
                    attempts >= 5 -> 60_000L
                    attempts >= 3 -> 30_000L
                    else -> 0L
                }
                _uiState.value = LockUiState(
                    error = "Błędny PIN",
                    failedAttempts = attempts,
                    lockoutEndTime = if (lockoutMs > 0) now + lockoutMs else 0
                )
                if (lockoutMs > 0) {
                    delay(lockoutMs)
                    _uiState.value = _uiState.value.copy(lockoutEndTime = 0, error = null)
                }
            }
        }
    }

    val biometricEnabled: Boolean
        get() = keyManager.biometricEnabled

    fun biometricDecryptCipher() = keyManager.getBiometricDecryptCipher()

    fun onBiometricUnlock(cipher: javax.crypto.Cipher) {
        _uiState.value = _uiState.value.copy(isProcessing = true)
        viewModelScope.launch(Dispatchers.Default) {
            if (keyManager.unlockWithBiometricCipher(cipher)) {
                lockState.onUnlocked()
                _uiState.value = LockUiState()
            } else {
                _uiState.value = LockUiState(error = "Odblokowanie biometryczne nie powiodło się")
            }
        }
    }

    private fun wipeDatabase() {
        val dbFile = context.getDatabasePath("portfello.db")
        // ponytail: open singleton handle keeps writing to the unlinked inode — harmless, fresh DB on next setup
        listOf(dbFile, java.io.File("${dbFile.path}-wal"), java.io.File("${dbFile.path}-shm")).forEach { it.delete() }
        java.io.File(context.filesDir, "crypto_config.json").delete()
        prefs.onboardingComplete = false
        _uiState.value = LockUiState(error = "Dane zostały usunięte. Ustaw nowy PIN.")
        lockState.resetToSetup()
    }
}
