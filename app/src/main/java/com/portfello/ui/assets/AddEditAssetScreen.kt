package com.portfello.ui.assets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.portfello.data.bullion.BullionCoinCatalog
import com.portfello.data.db.entity.AssetType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditAssetScreen(
    assetId: Long?,
    onDone: () -> Unit,
    viewModel: AddEditAssetViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(assetId) {
        if (assetId != null) viewModel.loadForEdit(assetId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEdit) "Edytuj aktywo" else "Dodaj aktywo") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wstecz")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!state.isEdit) {
                Text("Typ", style = MaterialTheme.typography.labelMedium)
                AssetTypePicker(state.type) { viewModel.update { copy(type = it) } }
            }

            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.update { copy(name = it) } },
                label = { Text("Nazwa") },
                modifier = Modifier.fillMaxWidth()
            )

            // Type-specific fields
            when (state.type) {
                AssetType.STOCK, AssetType.BOND_TRADED -> {
                    TickerSearchField(state, viewModel)
                    OutlinedTextField(
                        value = state.currency,
                        onValueChange = { viewModel.update { copy(currency = it.uppercase()) } },
                        label = { Text("Waluta") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                AssetType.CURRENCY -> {
                    OutlinedTextField(
                        value = state.tickerOrId,
                        onValueChange = { viewModel.update { copy(tickerOrId = it.uppercase()) } },
                        label = { Text("Kod waluty ISO (np. USD, EUR)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                AssetType.CRYPTO -> {
                    TickerSearchField(state, viewModel)
                }
                AssetType.METAL_BULLION -> {
                    BullionFields(state, viewModel)
                }
                AssetType.BOND_RETAIL -> {
                    BondRetailFields(state, viewModel)
                }
                AssetType.MANUAL -> {
                    OutlinedTextField(
                        value = state.manualValue,
                        onValueChange = { viewModel.update { copy(manualValue = it) } },
                        label = { Text("Aktualna wartość") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.currency,
                        onValueChange = { viewModel.update { copy(currency = it.uppercase()) } },
                        label = { Text("Waluta") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            OutlinedTextField(
                value = state.quantity,
                onValueChange = { viewModel.update { copy(quantity = it) } },
                label = { Text("Ilość / Jednostki") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.purchasePrice,
                onValueChange = { viewModel.update { copy(purchasePrice = it) } },
                label = { Text("Cena zakupu (opcjonalnie)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.notes,
                onValueChange = { viewModel.update { copy(notes = it) } },
                label = { Text("Notatki (opcjonalnie)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = { viewModel.save(onDone) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving
            ) {
                Text(if (state.isEdit) "Zapisz" else "Dodaj aktywo")
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AssetTypePicker(selected: AssetType, onSelect: (AssetType) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AssetType.entries.forEach { type ->
            FilterChip(
                selected = selected == type,
                onClick = { onSelect(type) },
                label = { Text(typeChipLabel(type)) }
            )
        }
    }
}

private fun typeChipLabel(type: AssetType) = when (type) {
    AssetType.STOCK -> "Akcje/ETF"
    AssetType.BOND_RETAIL -> "Obl. detaliczne"
    AssetType.BOND_TRADED -> "Obl. giełdowe"
    AssetType.CURRENCY -> "Waluta"
    AssetType.METAL_BULLION -> "Kruszec"
    AssetType.CRYPTO -> "Krypto"
    AssetType.MANUAL -> "Ręczne"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TickerSearchField(state: AddEditState, viewModel: AddEditAssetViewModel) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded && state.searchResults.isNotEmpty(), onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = state.searchQuery.ifEmpty { state.tickerOrId },
            onValueChange = {
                viewModel.search(it)
                expanded = true
            },
            label = { Text(if (state.type == AssetType.CRYPTO) "Szukaj kryptowaluty" else "Ticker") },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable, true),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )
        ExposedDropdownMenu(expanded = expanded && state.searchResults.isNotEmpty(), onDismissRequest = { expanded = false }) {
            state.searchResults.forEach { result ->
                DropdownMenuItem(
                    text = { Text("${result.symbol} — ${result.name}") },
                    onClick = {
                        viewModel.selectSearchResult(result)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun BullionFields(state: AddEditState, viewModel: AddEditAssetViewModel) {
    val metals = listOf("Au", "Ag", "Pt", "Pd")
    Text("Metal", style = MaterialTheme.typography.labelMedium) // ponytail: "Metal" same in PL
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        metals.forEachIndexed { idx, metal ->
            SegmentedButton(
                selected = state.metalType == metal,
                onClick = { viewModel.update { copy(metalType = metal) } },
                shape = SegmentedButtonDefaults.itemShape(idx, metals.size)
            ) { Text(metal) }
        }
    }

    Text("Popularne monety", style = MaterialTheme.typography.labelMedium)
    val coins = BullionCoinCatalog.byMetal(state.metalType)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        coins.forEach { coin ->
            FilterChip(
                selected = state.selectedCoin == coin,
                onClick = { viewModel.selectCoin(coin) },
                label = { Text(coin.name, style = MaterialTheme.typography.bodySmall) }
            )
        }
        FilterChip(
            selected = state.selectedCoin == null && state.bullionWeight.isNotEmpty(),
            onClick = { viewModel.update { copy(selectedCoin = null, name = "", bullionWeight = "", bullionPurity = "") } },
            label = { Text("Własny") }
        )
    }

    OutlinedTextField(
        value = state.bullionWeight,
        onValueChange = { viewModel.update { copy(bullionWeight = it) } },
        label = { Text("Waga całkowita (gramy)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        enabled = state.selectedCoin == null
    )
    OutlinedTextField(
        value = state.bullionPurity,
        onValueChange = { viewModel.update { copy(bullionPurity = it) } },
        label = { Text("Próba (np. 0.9999)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        enabled = state.selectedCoin == null
    )
    OutlinedTextField(
        value = state.bullionPremium,
        onValueChange = { viewModel.update { copy(bullionPremium = it) } },
        label = { Text("Premia (PLN za sztukę)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BondRetailFields(state: AddEditState, viewModel: AddEditAssetViewModel) {
    OutlinedTextField(
        value = state.bondSeries,
        onValueChange = { viewModel.update { copy(bondSeries = it.uppercase()) } },
        label = { Text("Seria (np. ROR0725)") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = state.bondNominal,
        onValueChange = { viewModel.update { copy(bondNominal = it) } },
        label = { Text("Wartość nominalna (PLN)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = state.bondRate,
        onValueChange = { viewModel.update { copy(bondRate = it) } },
        label = { Text("Aktualne oprocentowanie (%)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )

    val capTypes = listOf("ANNUAL" to "Kapitalizacja roczna", "END" to "Jednorazowa na koniec")
    Text("Kapitalizacja", style = MaterialTheme.typography.labelMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        capTypes.forEach { (value, label) ->
            FilterChip(
                selected = state.bondCapType == value,
                onClick = { viewModel.update { copy(bondCapType = value) } },
                label = { Text(label) }
            )
        }
    }
}
