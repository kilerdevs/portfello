package com.portfello.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfello.R
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
    val biometricEnabled: Boolean = false,
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
        wipeAfterAttempts = prefs.wipeAfterAttempts,
        biometricEnabled = keyManager.biometricEnabled
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
        _state.value = s.copy(message = context.getString(R.string.settings_saved))
    }

    fun biometricEncryptCipher() = keyManager.getBiometricEncryptCipher()

    fun enableBiometric(cipher: javax.crypto.Cipher) {
        val ok = keyManager.enableBiometric(cipher)
        _state.value = _state.value.copy(
            biometricEnabled = ok,
            message = context.getString(if (ok) R.string.biometric_enabled_msg else R.string.biometric_enable_failed)
        )
    }

    fun disableBiometric() {
        keyManager.disableBiometric()
        _state.value = _state.value.copy(biometricEnabled = false, message = context.getString(R.string.biometric_disabled_msg))
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
                        // new PIN derives a new DB key, so the old wrapped biometric key is void
                        biometricEnabled = keyManager.biometricEnabled,
                        message = context.getString(R.string.pin_changed_restart)
                    )
                } catch (e: Exception) {
                    _state.value = s.copy(
                        showChangePinDialog = false, oldPin = "", newPin = "",
                        message = context.getString(R.string.pin_change_failed_fmt, e.message)
                    )
                }
            } else {
                _state.value = s.copy(
                    showChangePinDialog = false, oldPin = "", newPin = "",
                    message = context.getString(R.string.wrong_current_pin)
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
            val tempExport = File(context.cacheDir, "portfello_export.db")
            try {
                tempExport.delete()
                // canonical SQLCipher backup: sqlcipher_export through the live
                // connection sees WAL content and the app keeps running afterwards
                val live = db.openHelper.writableDatabase
                live.execSQL(
                    "ATTACH DATABASE ? AS backup KEY ?",
                    arrayOf(tempExport.absolutePath, password)
                )
                try {
                    live.query("SELECT sqlcipher_export('backup')").use { it.moveToFirst() }
                } finally {
                    live.execSQL("DETACH DATABASE backup")
                }

                val out = context.contentResolver.openOutputStream(uri, "wt")
                    ?: error("openOutputStream returned null")
                out.use { FileInputStream(tempExport).use { input -> input.copyTo(it) } }
                _state.value = _state.value.copy(message = context.getString(R.string.export_done))
            } catch (e: Exception) {
                _state.value = _state.value.copy(message = context.getString(R.string.export_failed_fmt, e.message))
            } finally {
                tempExport.delete()
            }
        }
    }

    private fun importBackup(uri: Uri, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val tempImport = File(context.cacheDir, "portfello_import.db")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempImport).use { input.copyTo(it) }
                } ?: error("openInputStream returned null")

                // opening verifies the password (throws on mismatch), then rekey
                // to the current DB key so the app can open it after restart
                val rekey = SQLiteDatabase.openDatabase(
                    tempImport.absolutePath, password,
                    null, SQLiteDatabase.OPEN_READWRITE, null, null
                )
                try {
                    rekey.rawQuery("SELECT count(*) FROM sqlite_master", null).use { it.moveToFirst() }
                    rekey.changePassword(keyManager.getDatabaseKey())
                } finally {
                    rekey.close()
                }

                db.close()
                val dbPath = context.getDatabasePath("portfello.db")
                tempImport.copyTo(dbPath, overwrite = true)
                // stale journal files from the old database would corrupt the imported one
                File("${dbPath.path}-wal").delete()
                File("${dbPath.path}-shm").delete()
                _state.value = _state.value.copy(message = context.getString(R.string.import_done_restart))
            } catch (e: Exception) {
                _state.value = _state.value.copy(message = context.getString(R.string.import_failed_fmt, e.message))
            } finally {
                tempImport.delete()
            }
        }
    }

    fun lock() {
        lockState.lock()
    }
}
