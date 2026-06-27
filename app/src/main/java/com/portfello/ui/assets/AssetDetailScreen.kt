package com.portfello.ui.assets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AssetDetailScreen(
    assetId: Long,
    onEdit: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: AssetDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(assetId) { viewModel.load(assetId) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Usunąć aktywo?") },
            text = { Text("To trwale usunie to aktywo i wszystkie powiązane dane.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteAsset(onBack) }) {
                    Text("Usuń", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Anuluj") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.valuation?.asset?.name ?: "Aktywo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wstecz")
                    }
                },
                actions = {
                    IconButton(onClick = { onEdit(assetId) }) {
                        Icon(Icons.Default.Edit, "Edytuj")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, "Usuń")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Column(
                Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        val valuation = state.valuation ?: return@Scaffold

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Wartość", style = MaterialTheme.typography.labelMedium)
                    Text(
                        formatValue(valuation.totalValue, valuation.baseCurrency),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    valuation.pricePerUnit?.let { price ->
                        Text(
                            "Cena: ${formatValue(price, valuation.priceCurrency ?: "")}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    valuation.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    valuation.lastUpdated?.let {
                        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        Text("Aktualizacja: ${fmt.format(Date(it))}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (state.priceHistory.isNotEmpty()) {
                val ranges = listOf(1 to "1D", 7 to "7D", 30 to "1M", 90 to "3M", 365 to "1Y")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ranges.forEach { (days, label) ->
                        FilterChip(
                            selected = state.selectedRange == days,
                            onClick = { viewModel.selectRange(days) },
                            label = { Text(label) }
                        )
                    }
                }
                PriceChart(
                    prices = state.priceHistory.map { it.price },
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
            }

            Text("Pozycje", style = MaterialTheme.typography.titleSmall)
            state.holdings.forEach { h ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        val qtyStr = if (h.quantity == h.quantity.toLong().toDouble()) h.quantity.toLong().toString() else h.quantity.toString()
                        Text("Ilość: $qtyStr")
                        h.purchasePrice?.let {
                            Text("@ ${formatValue(it, valuation.asset.currency)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            valuation.asset.notes?.let {
                Text("Notatki", style = MaterialTheme.typography.titleSmall)
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private val compactAxisFormat = CartesianValueFormatter { _, value, _ ->
    when {
        value >= 1_000_000 -> "%.1fM".format(value / 1_000_000)
        value >= 1_000 -> "%.0fk".format(value / 1_000)
        else -> DecimalFormat("#.##").format(value)
    }
}

@Composable
private fun PriceChart(prices: List<Double>, modifier: Modifier = Modifier) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(prices) {
        if (prices.isNotEmpty()) {
            modelProducer.runTransaction {
                lineSeries { series(prices) }
            }
        }
    }
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(valueFormatter = compactAxisFormat),
            bottomAxis = HorizontalAxis.rememberBottom()
        ),
        modelProducer = modelProducer,
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content),
        modifier = modifier
    )
}
