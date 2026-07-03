package com.portfello.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.portfello.R
import com.portfello.ui.common.canUseBiometrics
import com.portfello.ui.common.showBiometricPrompt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val logEntries by viewModel.networkLog.entries.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri -> uri?.let { viewModel.onExportUriReceived(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.onImportUriReceived(it) } }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHost.showSnackbar(it)
            viewModel.update { copy(message = null) }
        }
    }

    if (state.showChangePinDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.update { copy(showChangePinDialog = false) } },
            title = { Text(stringResource(R.string.change_pin_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.oldPin,
                        onValueChange = { viewModel.update { copy(oldPin = it) } },
                        label = { Text(stringResource(R.string.current_pin)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    OutlinedTextField(
                        value = state.newPin,
                        onValueChange = { viewModel.update { copy(newPin = it) } },
                        label = { Text(stringResource(R.string.new_pin)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.changePin() }) { Text(stringResource(R.string.change)) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.update { copy(showChangePinDialog = false) } }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (state.showExportPasswordDialog || state.showImportPasswordDialog) {
        val isExport = state.showExportPasswordDialog
        AlertDialog(
            onDismissRequest = {
                viewModel.update { copy(showExportPasswordDialog = false, showImportPasswordDialog = false, pendingUri = null) }
            },
            title = { Text(stringResource(if (isExport) R.string.backup_password_title else R.string.import_password_title)) },
            text = {
                OutlinedTextField(
                    value = state.backupPassword,
                    onValueChange = { viewModel.update { copy(backupPassword = it) } },
                    label = { Text(stringResource(R.string.password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { if (isExport) viewModel.confirmExport() else viewModel.confirmImport() },
                    enabled = state.backupPassword.length >= 4
                ) { Text(stringResource(if (isExport) R.string.export else R.string.import_label)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.update { copy(showExportPasswordDialog = false, showImportPasswordDialog = false, pendingUri = null) }
                }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader(stringResource(R.string.section_general))

            OutlinedTextField(
                value = state.baseCurrency,
                onValueChange = { viewModel.update { copy(baseCurrency = it.uppercase()) } },
                label = { Text(stringResource(R.string.base_currency)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.syncIntervalMin.toString(),
                onValueChange = { viewModel.update { copy(syncIntervalMin = it.toLongOrNull() ?: 240) } },
                label = { Text(stringResource(R.string.sync_interval_min)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.coinGeckoApiKey,
                onValueChange = { viewModel.update { copy(coinGeckoApiKey = it) } },
                label = { Text(stringResource(R.string.coingecko_key_opt)) },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()
            SectionHeader(stringResource(R.string.section_security))

            OutlinedTextField(
                value = state.lockTimeoutSec.toString(),
                onValueChange = { viewModel.update { copy(lockTimeoutSec = it.toLongOrNull() ?: 60) } },
                label = { Text(stringResource(R.string.lock_after_sec)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = if (state.wipeAfterAttempts > 0) state.wipeAfterAttempts.toString() else "",
                onValueChange = { viewModel.update { copy(wipeAfterAttempts = it.toIntOrNull() ?: 0) } },
                label = { Text(stringResource(R.string.wipe_after_attempts)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            val context = LocalContext.current
            val enrollTitle = stringResource(R.string.biometric_enroll_title)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.biometric_unlock), style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = state.biometricEnabled,
                    enabled = canUseBiometrics(context),
                    onCheckedChange = { wantEnabled ->
                        if (!wantEnabled) {
                            viewModel.disableBiometric()
                        } else {
                            val activity = context as? FragmentActivity ?: return@Switch
                            val cipher = viewModel.biometricEncryptCipher() ?: return@Switch
                            showBiometricPrompt(activity, cipher, enrollTitle) {
                                viewModel.enableBiometric(it)
                            }
                        }
                    }
                )
            }

            Button(onClick = { viewModel.saveAll() }, Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.save_settings))
            }

            Spacer(Modifier.height(4.dp))

            OutlinedButton(
                onClick = { viewModel.update { copy(showChangePinDialog = true) } },
                Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.change_pin))
            }
            OutlinedButton(onClick = { viewModel.lock() }, Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.lock_now))
            }

            HorizontalDivider()
            SectionHeader(stringResource(R.string.section_backup))
            Text(
                stringResource(R.string.backup_info),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { exportLauncher.launch("portfello_backup.db") },
                    Modifier.weight(1f)
                ) { Text(stringResource(R.string.export)) }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/octet-stream", "*/*")) },
                    Modifier.weight(1f)
                ) { Text(stringResource(R.string.import_label)) }
            }

            HorizontalDivider()
            SectionHeader(stringResource(R.string.section_net_log))

            if (logEntries.isEmpty()) {
                Text(
                    stringResource(R.string.net_log_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val listState = rememberLazyListState()
                LaunchedEffect(logEntries.size) {
                    if (logEntries.isNotEmpty()) listState.animateScrollToItem(logEntries.lastIndex)
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    items(logEntries) { entry ->
                        val color = when {
                            entry.contains("FAIL") || entry.contains("ERR") -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Text(
                            entry,
                            style = MaterialTheme.typography.labelSmall,
                            color = color,
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        )
                    }
                }
                OutlinedButton(onClick = { viewModel.networkLog.clear() }, Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.clear_log))
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}
