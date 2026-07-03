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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.portfello.R
import com.portfello.data.bullion.BullionCoinCatalog
import com.portfello.data.db.entity.AssetType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditAssetScreen(
    assetId: Long?,
    onDone: () -> Unit,
    viewModel: AddEditAssetViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(assetId) {
        if (assetId != null) viewModel.loadForEdit(assetId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isEdit) R.string.edit_asset else R.string.add_asset)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(start = 16.dp, top = 16.dp, end = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!state.isEdit) {
                Text(stringResource(R.string.type), style = MaterialTheme.typography.labelMedium)
                AssetTypePicker(state.type) { viewModel.update { copy(type = it) } }
            }

            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.update { copy(name = it) } },
                label = { Text(stringResource(R.string.name)) },
                modifier = Modifier.fillMaxWidth()
            )

            // Type-specific fields
            when (state.type) {
                AssetType.STOCK, AssetType.BOND_TRADED -> {
                    TickerSearchField(state, viewModel)
                    OutlinedTextField(
                        value = state.currency,
                        onValueChange = { viewModel.update { copy(currency = it.uppercase()) } },
                        label = { Text(stringResource(R.string.currency)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                AssetType.CURRENCY -> {
                    OutlinedTextField(
                        value = state.tickerOrId,
                        onValueChange = { viewModel.update { copy(tickerOrId = it.uppercase()) } },
                        label = { Text(stringResource(R.string.currency_iso)) },
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
                        label = { Text(stringResource(R.string.current_value)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.currency,
                        onValueChange = { viewModel.update { copy(currency = it.uppercase()) } },
                        label = { Text(stringResource(R.string.currency)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            OutlinedTextField(
                value = state.quantity,
                onValueChange = { viewModel.update { copy(quantity = it) } },
                label = { Text(stringResource(R.string.quantity_units)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.purchasePrice,
                onValueChange = { viewModel.update { copy(purchasePrice = it) } },
                label = { Text(stringResource(R.string.purchase_price_opt)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.notes,
                onValueChange = { viewModel.update { copy(notes = it) } },
                label = { Text(stringResource(R.string.notes_opt)) },
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
                Text(stringResource(if (state.isEdit) R.string.save else R.string.add_asset))
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

@Composable
private fun typeChipLabel(type: AssetType) = stringResource(when (type) {
    AssetType.STOCK -> R.string.type_stock_chip
    AssetType.BOND_RETAIL -> R.string.type_bond_retail_chip
    AssetType.BOND_TRADED -> R.string.type_bond_traded_chip
    AssetType.CURRENCY -> R.string.type_currency_chip
    AssetType.METAL_BULLION -> R.string.type_metal_chip
    AssetType.CRYPTO -> R.string.type_crypto_chip
    AssetType.MANUAL -> R.string.type_manual_chip
})

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
            label = { Text(stringResource(if (state.type == AssetType.CRYPTO) R.string.search_crypto else R.string.ticker)) },
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
    Text(stringResource(R.string.metal), style = MaterialTheme.typography.labelMedium)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        metals.forEachIndexed { idx, metal ->
            SegmentedButton(
                selected = state.metalType == metal,
                onClick = { viewModel.update { copy(metalType = metal) } },
                shape = SegmentedButtonDefaults.itemShape(idx, metals.size)
            ) { Text(metal) }
        }
    }

    Text(stringResource(R.string.popular_coins), style = MaterialTheme.typography.labelMedium)
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
            label = { Text(stringResource(R.string.custom_coin)) }
        )
    }

    OutlinedTextField(
        value = state.bullionWeight,
        onValueChange = { viewModel.update { copy(bullionWeight = it) } },
        label = { Text(stringResource(R.string.total_weight_grams)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        enabled = state.selectedCoin == null
    )
    OutlinedTextField(
        value = state.bullionPurity,
        onValueChange = { viewModel.update { copy(bullionPurity = it) } },
        label = { Text(stringResource(R.string.purity_hint)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        enabled = state.selectedCoin == null
    )
    OutlinedTextField(
        value = state.bullionPremium,
        onValueChange = { viewModel.update { copy(bullionPremium = it) } },
        label = { Text(stringResource(R.string.premium_pln)) },
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
        label = { Text(stringResource(R.string.bond_series_hint)) },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = state.bondNominal,
        onValueChange = { viewModel.update { copy(bondNominal = it) } },
        label = { Text(stringResource(R.string.bond_nominal_pln)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = state.bondRate,
        onValueChange = { viewModel.update { copy(bondRate = it) } },
        label = { Text(stringResource(R.string.bond_rate_pct)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )

    val capTypes = listOf("ANNUAL" to stringResource(R.string.cap_annual), "END" to stringResource(R.string.cap_end))
    Text(stringResource(R.string.capitalization), style = MaterialTheme.typography.labelMedium)
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
