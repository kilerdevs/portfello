package com.portfello.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.portfello.R
import com.portfello.data.db.entity.AssetType
import com.portfello.domain.AssetValuation
import com.portfello.ui.common.ChangeBadge
import com.portfello.ui.common.ProfitLossText
import com.portfello.ui.common.TimeLineChart
import com.portfello.ui.common.formatMoney
import com.portfello.ui.theme.ElectricTeal
import com.portfello.ui.theme.Violet
import com.portfello.ui.theme.typeColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    onNavigateToAssets: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onAssetClick: (Long) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, stringResource(R.string.refresh))
                    }
                    IconButton(onClick = onNavigateToAssets) {
                        Icon(Icons.AutoMirrored.Filled.List, stringResource(R.string.assets))
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, stringResource(R.string.settings))
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
                    stringResource(R.string.portfolio_empty),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.portfolio_empty_hint),
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
                HeroCard(state)
            }

            if (state.allocation.isNotEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.allocation), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(16.dp))
                            AllocationDonut(
                                allocation = state.allocation,
                                total = state.totalValue,
                                modifier = Modifier.size(170.dp)
                            )
                            Spacer(Modifier.height(16.dp))
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
                        Text(stringResource(R.string.value_over_time), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            TimeLineChart(
                                points = state.chartData.map { it.timestamp to it.value },
                                modifier = Modifier.fillMaxWidth().height(200.dp)
                            )
                        } else {
                            Text(
                                stringResource(R.string.no_chart_data),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Text(stringResource(R.string.positions), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
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
                            Text(formatMoney(v.totalValue, state.baseCurrency), style = MaterialTheme.typography.titleMedium)
                            ChangeBadge(v.change24hPct)
                            ProfitLossText(v.profitLoss, v.profitLossPct, state.baseCurrency, style = MaterialTheme.typography.labelSmall)
                            v.error?.let {
                                Text(stringResource(R.string.offline), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            if (sorted.size > 10) {
                item {
                    Text(
                        stringResource(R.string.more_positions, sorted.size - 10),
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
private fun HeroCard(state: DashboardState) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(ElectricTeal.copy(alpha = 0.22f), Violet.copy(alpha = 0.22f))
                ),
                RoundedCornerShape(20.dp)
            )
            .padding(20.dp)
    ) {
        Column {
            Text(
                stringResource(R.string.portfolio_value),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                formatMoney(state.totalValue, state.baseCurrency),
                style = MaterialTheme.typography.headlineLarge
            )
            if (state.change24hPct != null || state.change7dPct != null) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChangeBadge(state.change24hPct, label = "24h")
                    ChangeBadge(state.change7dPct, label = "7d")
                }
            }
            val costs = state.valuations.mapNotNull { it.costBasisInBase }
            if (costs.isNotEmpty()) {
                val totalCost = costs.sum()
                val coveredValue = state.valuations
                    .filter { it.costBasisInBase != null }
                    .sumOf { it.totalValue }
                Spacer(Modifier.height(4.dp))
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

@Composable
private fun AllocationDonut(allocation: Map<AssetType, Double>, total: Double, modifier: Modifier) {
    Canvas(modifier.aspectRatio(1f)) {
        if (total <= 0) return@Canvas
        val strokeWidth = size.width * 0.16f
        val inset = strokeWidth / 2
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        var startAngle = -90f
        allocation.forEach { (type, value) ->
            val sweep = (value / total * 360).toFloat()
            drawArc(
                color = typeColors[type] ?: Color.Gray,
                startAngle = startAngle,
                // small gap between segments
                sweepAngle = (sweep - 2f).coerceAtLeast(0.5f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )
            startAngle += sweep
        }
    }
}

@Composable
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

@Composable
private fun typeShortLabel(type: AssetType) = stringResource(when (type) {
    AssetType.STOCK -> R.string.type_stock_short
    AssetType.BOND_RETAIL -> R.string.type_bond_retail_short
    AssetType.BOND_TRADED -> R.string.type_bond_traded_short
    AssetType.CURRENCY -> R.string.type_currency_short
    AssetType.METAL_BULLION -> R.string.type_metal_short
    AssetType.CRYPTO -> R.string.type_crypto_short
    AssetType.MANUAL -> R.string.type_manual_short
})
