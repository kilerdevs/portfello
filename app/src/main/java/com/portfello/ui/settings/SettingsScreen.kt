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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val logEntries by viewModel.networkLog.entries.collectAsState()
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
            title = { Text("Zmiana PIN-u") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.oldPin,
                        onValueChange = { viewModel.update { copy(oldPin = it) } },
                        label = { Text("Aktualny PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    OutlinedTextField(
                        value = state.newPin,
                        onValueChange = { viewModel.update { copy(newPin = it) } },
                        label = { Text("Nowy PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.changePin() }) { Text("Zmień") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.update { copy(showChangePinDialog = false) } }) { Text("Anuluj") }
            }
        )
    }

    if (state.showExportPasswordDialog || state.showImportPasswordDialog) {
        val isExport = state.showExportPasswordDialog
        AlertDialog(
            onDismissRequest = {
                viewModel.update { copy(showExportPasswordDialog = false, showImportPasswordDialog = false, pendingUri = null) }
            },
            title = { Text(if (isExport) "Hasło kopii zapasowej" else "Hasło importu") },
            text = {
                OutlinedTextField(
                    value = state.backupPassword,
                    onValueChange = { viewModel.update { copy(backupPassword = it) } },
                    label = { Text("Hasło") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { if (isExport) viewModel.confirmExport() else viewModel.confirmImport() },
                    enabled = state.backupPassword.length >= 4
                ) { Text(if (isExport) "Eksportuj" else "Importuj") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.update { copy(showExportPasswordDialog = false, showImportPasswordDialog = false, pendingUri = null) }
                }) { Text("Anuluj") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ustawienia") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wstecz")
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
            SectionHeader("Ogólne")

            OutlinedTextField(
                value = state.baseCurrency,
                onValueChange = { viewModel.update { copy(baseCurrency = it.uppercase()) } },
                label = { Text("Waluta bazowa") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.syncIntervalMin.toString(),
                onValueChange = { viewModel.update { copy(syncIntervalMin = it.toLongOrNull() ?: 240) } },
                label = { Text("Interwał synchronizacji (min)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.coinGeckoApiKey,
                onValueChange = { viewModel.update { copy(coinGeckoApiKey = it) } },
                label = { Text("Klucz CoinGecko (opcjonalnie)") },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()
            SectionHeader("Bezpieczeństwo")

            OutlinedTextField(
                value = state.lockTimeoutSec.toString(),
                onValueChange = { viewModel.update { copy(lockTimeoutSec = it.toLongOrNull() ?: 60) } },
                label = { Text("Blokada po (sekundy nieaktywności)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = if (state.wipeAfterAttempts > 0) state.wipeAfterAttempts.toString() else "",
                onValueChange = { viewModel.update { copy(wipeAfterAttempts = it.toIntOrNull() ?: 0) } },
                label = { Text("Kasuj bazę po N błędnych PIN-ach (0 = wył.)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Button(onClick = { viewModel.saveAll() }, Modifier.fillMaxWidth()) {
                Text("Zapisz ustawienia")
            }

            Spacer(Modifier.height(4.dp))

            OutlinedButton(
                onClick = { viewModel.update { copy(showChangePinDialog = true) } },
                Modifier.fillMaxWidth()
            ) {
                Text("Zmień PIN")
            }
            OutlinedButton(onClick = { viewModel.lock() }, Modifier.fillMaxWidth()) {
                Text("Zablokuj teraz")
            }

            HorizontalDivider()
            SectionHeader("Kopia zapasowa")
            Text(
                "Eksport/Import tworzy lub przywraca zaszyfrowaną kopię zapasową.\nKopie chronione są osobnym hasłem.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { exportLauncher.launch("portfello_backup.db") },
                    Modifier.weight(1f)
                ) { Text("Eksportuj") }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/octet-stream", "*/*")) },
                    Modifier.weight(1f)
                ) { Text("Importuj") }
            }

            HorizontalDivider()
            SectionHeader("Log sieciowy")

            if (logEntries.isEmpty()) {
                Text(
                    "Brak wpisów — pojawią się po pierwszej synchronizacji.",
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
                    Text("Wyczyść log")
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
