package com.portfello.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
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
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.portfello.data.db.entity.AssetType
import com.portfello.domain.AssetValuation
import com.portfello.ui.assets.formatValue
import com.portfello.ui.common.ProfitLossText

private val typeColors = mapOf(
    AssetType.STOCK to Color(0xFF6750A4),
    AssetType.BOND_RETAIL to Color(0xFF625B71),
    AssetType.BOND_TRADED to Color(0xFF7D5260),
    AssetType.CURRENCY to Color(0xFF006D40),
    AssetType.METAL_BULLION to Color(0xFFFFB900),
    AssetType.CRYPTO to Color(0xFFE8600A),
    AssetType.MANUAL to Color(0xFF49454F),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    onNavigateToAssets: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onAssetClick: (Long) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Portfello") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, "Odśwież")
                    }
                    IconButton(onClick = onNavigateToAssets) {
                        Icon(Icons.AutoMirrored.Filled.List, "Aktywa")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Ustawienia")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading && !state.isRefreshing) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (state.valuations.isEmpty() && !state.isRefreshing) {
            Column(
                Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Outlined.Star,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Portfel jest pusty",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Przejdź do Aktywów, aby dodać pierwsze",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Wartość portfela", style = MaterialTheme.typography.labelMedium)
                        Text(
                            formatValue(state.totalValue, state.baseCurrency),
                            style = MaterialTheme.typography.headlineLarge
                        )
                        val costs = state.valuations.mapNotNull { it.costBasisInBase }
                        if (costs.isNotEmpty()) {
                            val totalCost = costs.sum()
                            val coveredValue = state.valuations
                                .filter { it.costBasisInBase != null }
                                .sumOf { it.totalValue }
                            ProfitLossText(
                                profitLoss = coveredValue - totalCost,
                                profitLossPct = if (totalCost != 0.0) (coveredValue - totalCost) / totalCost * 100 else null,
                                currency = state.baseCurrency,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            if (state.allocation.isNotEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Alokacja", style = MaterialTheme.typography.labelMedium, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(12.dp))
                            AllocationPie(
                                allocation = state.allocation,
                                total = state.totalValue,
                                modifier = Modifier.size(160.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                state.allocation.forEach { (type, value) ->
                                    val pct = if (state.totalValue > 0) (value / state.totalValue * 100) else 0.0
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Canvas(Modifier.size(10.dp)) {
                                            drawCircle(typeColors[type] ?: Color.Gray)
                                        }
                                        Text(
                                            " ${typeShortLabel(type)} %.1f%%".format(pct),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Wartość w czasie", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(8.dp))
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
                        Spacer(Modifier.height(8.dp))
                        if (state.chartData.size > 1) {
                            PortfolioChart(
                                data = state.chartData,
                                modifier = Modifier.fillMaxWidth().height(200.dp)
                            )
                        } else {
                            Text(
                                "Brak danych historycznych dla wybranego okresu",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Text("Pozycje", style = MaterialTheme.typography.titleSmall)
            }
            val sorted = state.valuations.sortedByDescending { it.totalValue }
            items(sorted.take(10), key = { it.asset.id }) { v ->
                Card(
                    Modifier.fillMaxWidth().clickable { onAssetClick(v.asset.id) }
                ) {
                    Row(
                        Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(v.asset.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                positionSubtitle(v),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(formatValue(v.totalValue, state.baseCurrency), style = MaterialTheme.typography.titleMedium)
                            ProfitLossText(v.profitLoss, v.profitLossPct, state.baseCurrency, style = MaterialTheme.typography.labelSmall)
                            v.error?.let {
                                Text("Offline", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            if (sorted.size > 10) {
                item {
                    Text(
                        "… i ${sorted.size - 10} więcej — przejdź do Aktywów",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun AllocationPie(allocation: Map<AssetType, Double>, total: Double, modifier: Modifier) {
    Canvas(modifier.aspectRatio(1f)) {
        if (total <= 0) return@Canvas
        var startAngle = -90f
        allocation.forEach { (type, value) ->
            val sweep = (value / total * 360).toFloat()
            drawArc(
                color = typeColors[type] ?: Color.Gray,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = true,
                topLeft = Offset.Zero,
                size = Size(size.width, size.height)
            )
            startAngle += sweep
        }
    }
}

private val yAxisFormat = CartesianValueFormatter { _, value, _ ->
    when {
        value >= 1_000_000 -> "%.1fM".format(value / 1_000_000)
        value >= 1_000 -> "%.0fk".format(value / 1_000)
        else -> DecimalFormat("#.##").format(value)
    }
}

@Composable
private fun PortfolioChart(data: List<ChartPoint>, modifier: Modifier) {
    val snapshot = remember(data) { data.toList() }
    val modelProducer = remember { CartesianChartModelProducer() }

    val dayMs = 24L * 3600 * 1000
    val baseDay = remember(snapshot) {
        if (snapshot.isNotEmpty()) snapshot.first().timestamp / dayMs else 0L
    }
    val spanDays = remember(snapshot) {
        if (snapshot.size < 2) 1L
        else (snapshot.last().timestamp - snapshot.first().timestamp) / dayMs
    }

    LaunchedEffect(snapshot) {
        if (snapshot.size >= 2) {
            modelProducer.runTransaction {
                lineSeries {
                    series(
                        x = snapshot.map { ((it.timestamp / dayMs) - baseDay).toDouble() },
                        y = snapshot.map { it.value },
                    )
                }
            }
        }
    }

    val xAxisFormat = remember(baseDay, spanDays) {
        val fmt = SimpleDateFormat(if (spanDays > 90) "MM.yy" else "dd.MM", Locale.getDefault())
        val today = System.currentTimeMillis() / dayMs
        CartesianValueFormatter { _, value, _ ->
            val day = baseDay + Math.round(value)
            if (day >= today) "Dziś" else fmt.format(Date(day * dayMs))
        }
    }

    // ponytail: getXStep controls label/tick density — one label per spanDays/5 days
    val xStep: (CartesianChartModel) -> Double = remember(spanDays) {
        { _ -> (spanDays / 5.0).coerceAtLeast(1.0) }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(valueFormatter = yAxisFormat),
            bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = xAxisFormat),
            getXStep = xStep,
        ),
        modelProducer = modelProducer,
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content),
        modifier = modifier
    )
}

private fun positionSubtitle(v: AssetValuation): String {
    val type = typeShortLabel(v.asset.type)
    return when (v.asset.type) {
        AssetType.STOCK, AssetType.BOND_TRADED -> {
            val t = v.asset.tickerOrId?.uppercase().orEmpty()
            if (t.isNotEmpty()) "$t · $type" else type
        }
        // ponytail: hide ugly CoinGecko IDs like "bitcoin-cash"
        else -> type
    }
}

private fun typeShortLabel(type: AssetType) = when (type) {
    AssetType.STOCK -> "Akcje"
    AssetType.BOND_RETAIL -> "Obligacje (D)"
    AssetType.BOND_TRADED -> "Obligacje (G)"
    AssetType.CURRENCY -> "Waluty"
    AssetType.METAL_BULLION -> "Metale"
    AssetType.CRYPTO -> "Krypto"
    AssetType.MANUAL -> "Ręczne"
}
