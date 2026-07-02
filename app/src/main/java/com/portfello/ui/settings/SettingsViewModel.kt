package com.portfello.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfello.data.AppPrefs
import com.portfello.data.crypto.KeyManager
import com.portfello.data.crypto.LockState
import com.portfello.data.db.PortfelloDatabase
import com.portfello.data.network.NetworkLog
import com.portfello.data.network.client.CoinGeckoClient
import com.portfello.data.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject

data class SettingsState(
    val baseCurrency: String = "PLN",
    val syncIntervalMin: Long = 240,
    val coinGeckoApiKey: String = "",
    val lockTimeoutSec: Long = 60,
    val wipeAfterAttempts: Int = 0,
    val message: String? = null,
    val showChangePinDialog: Boolean = false,
    val oldPin: String = "",
    val newPin: String = "",
    val showExportPasswordDialog: Boolean = false,
    val showImportPasswordDialog: Boolean = false,
    val backupPassword: String = "",
    val pendingUri: Uri? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val keyManager: KeyManager,
    private val lockState: LockState,
    private val syncScheduler: SyncScheduler,
    private val coinGeckoClient: CoinGeckoClient,
    private val db: PortfelloDatabase,
    private val prefs: AppPrefs,
    val networkLog: NetworkLog
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState(
        baseCurrency = prefs.baseCurrency,
        syncIntervalMin = prefs.syncIntervalMin,
        coinGeckoApiKey = prefs.coinGeckoApiKey,
        lockTimeoutSec = prefs.lockTimeoutSec,
        wipeAfterAttempts = prefs.wipeAfterAttempts
    ))
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    fun update(fn: SettingsState.() -> SettingsState) {
        _state.value = _state.value.fn()
    }

    fun saveAll() {
        val s = _state.value
        prefs.baseCurrency = s.baseCurrency
        prefs.syncIntervalMin = s.syncIntervalMin
        syncScheduler.schedule(s.syncIntervalMin)
        prefs.coinGeckoApiKey = s.coinGeckoApiKey
        coinGeckoClient.apiKey = s.coinGeckoApiKey.ifBlank { null }
        prefs.lockTimeoutSec = s.lockTimeoutSec
        lockState.lockTimeoutMs = s.lockTimeoutSec * 1000
        prefs.wipeAfterAttempts = s.wipeAfterAttempts
        _state.value = s.copy(message = "Ustawienia zapisane")
    }

    fun changePin() {
        val s = _state.value
        viewModelScope.launch(Dispatchers.IO) {
            val oldKey = keyManager.changePin(s.oldPin, s.newPin)
            if (oldKey != null) {
                try {
                    val newKey = keyManager.getDatabaseKey()
                    val dbPath = context.getDatabasePath("portfello.db").absolutePath
                    db.close()
                    val sqlDb = SQLiteDatabase.openDatabase(
                        dbPath,
                        oldKey,
                        null, SQLiteDatabase.OPEN_READWRITE, null, null
                    )
                    sqlDb.changePassword(newKey)
                    sqlDb.close()
                    oldKey.fill(0)
                    _state.value = s.copy(
                        showChangePinDialog = false, oldPin = "", newPin = "",
                        message = "PIN zmieniony — uruchom ponownie aplikację"
                    )
                } catch (e: Exception) {
                    _state.value = s.copy(
                        showChangePinDialog = false, oldPin = "", newPin = "",
                        message = "Zmiana PIN-u nie powiodła się: ${e.message}"
                    )
                }
            } else {
                _state.value = s.copy(
                    showChangePinDialog = false, oldPin = "", newPin = "",
                    message = "Błędny aktualny PIN"
                )
            }
        }
    }

    fun onExportUriReceived(uri: Uri) {
        _state.value = _state.value.copy(
            showExportPasswordDialog = true,
            pendingUri = uri,
            backupPassword = ""
        )
    }

    fun onImportUriReceived(uri: Uri) {
        _state.value = _state.value.copy(
            showImportPasswordDialog = true,
            pendingUri = uri,
            backupPassword = ""
        )
    }

    fun confirmExport() {
        val s = _state.value
        val uri = s.pendingUri ?: return
        val password = s.backupPassword
        _state.value = s.copy(showExportPasswordDialog = false, pendingUri = null, backupPassword = "")
        exportBackup(uri, password)
    }

    fun confirmImport() {
        val s = _state.value
        val uri = s.pendingUri ?: return
        val password = s.backupPassword
        _state.value = s.copy(showImportPasswordDialog = false, pendingUri = null, backupPassword = "")
        importBackup(uri, password)
    }

    private fun exportBackup(uri: Uri, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dbPath = context.getDatabasePath("portfello.db").absolutePath
                db.close()
                val tempExport = File(context.cacheDir, "portfello_export.db")
                File(dbPath).copyTo(tempExport, overwrite = true)

                val exportDb = SQLiteDatabase.openDatabase(
                    tempExport.absolutePath,
                    keyManager.getDatabaseKey(),
                    null, SQLiteDatabase.OPEN_READWRITE, null, null
                )
                exportDb.changePassword(password)
                exportDb.close()

                context.contentResolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(tempExport).use { it.copyTo(out) }
                }
                tempExport.delete()
                _state.value = _state.value.copy(message = "Kopia zapasowa wyeksportowana")
            } catch (e: Exception) {
                _state.value = _state.value.copy(message = "Eksport nie powiódł się: ${e.message}")
            }
        }
    }

    private fun importBackup(uri: Uri, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tempImport = File(context.cacheDir, "portfello_import.db")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempImport).use { input.copyTo(it) }
                }
                val testDb = SQLiteDatabase.openDatabase(
                    tempImport.absolutePath, password,
                    null, SQLiteDatabase.OPEN_READONLY, null, null
                )
                testDb.close()

                val rekey = SQLiteDatabase.openDatabase(
                    tempImport.absolutePath, password,
                    null, SQLiteDatabase.OPEN_READWRITE, null, null
                )
                rekey.changePassword(keyManager.getDatabaseKey())
                rekey.close()

                db.close()
                val dbPath = context.getDatabasePath("portfello.db")
                tempImport.copyTo(dbPath, overwrite = true)
                tempImport.delete()
                _state.value = _state.value.copy(message = "Kopia zaimportowana — uruchom ponownie")
            } catch (e: Exception) {
                _state.value = _state.value.copy(message = "Import nie powiódł się: ${e.message}")
            }
        }
    }

    fun lock() {
        lockState.lock()
    }
}
