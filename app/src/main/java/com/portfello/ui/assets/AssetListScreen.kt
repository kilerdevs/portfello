package com.portfello.ui.assets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.portfello.data.db.entity.AssetType
import com.portfello.domain.AssetValuation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetListScreen(
    onAddAsset: () -> Unit,
    onAssetClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: AssetListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aktywa") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wstecz")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAsset) {
                Icon(Icons.Default.Add, "Dodaj aktywo")
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Column(
                Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) { CircularProgressIndicator() }
        } else if (state.groups.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(Modifier.height(16.dp))
                Text("Brak aktywów", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Dodaj pierwsze przyciskiem +",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                state.groups.forEach { (type, valuations) ->
                    item {
                        Text(
                            typeLabel(type),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(valuations, key = { it.asset.id }) { valuation ->
                        AssetRow(valuation, state.baseCurrency) { onAssetClick(valuation.asset.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssetRow(valuation: AssetValuation, baseCurrency: String, onClick: () -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(valuation.asset.name, style = MaterialTheme.typography.bodyLarge)
                valuation.asset.tickerOrId?.let {
                    val display = if (valuation.asset.type == AssetType.CRYPTO) null else it.uppercase()
                    display?.let { d -> Text(d, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatValue(valuation.totalValue, baseCurrency),
                    style = MaterialTheme.typography.titleMedium
                )
                com.portfello.ui.common.ProfitLossText(valuation.profitLoss, valuation.profitLossPct, baseCurrency)
                valuation.error?.let {
                    Text("Offline", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) // ponytail: "Offline" universal, no translation needed
                }
            }
        }
    }
}

private fun typeLabel(type: AssetType) = when (type) {
    AssetType.STOCK -> "Akcje i ETF-y"
    AssetType.BOND_RETAIL -> "Obligacje detaliczne"
    AssetType.BOND_TRADED -> "Obligacje giełdowe"
    AssetType.CURRENCY -> "Waluty"
    AssetType.METAL_BULLION -> "Metale szlachetne"
    AssetType.CRYPTO -> "Kryptowaluty"
    AssetType.MANUAL -> "Ręczne"
}

internal fun formatValue(value: Double, currency: String): String {
    return "%,.2f %s".format(value, currency)
}
