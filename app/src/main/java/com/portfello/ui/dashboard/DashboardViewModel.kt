package com.portfello.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfello.data.AppPrefs
import com.portfello.data.db.dao.PortfolioSnapshotDao
import com.portfello.data.db.entity.AssetType
import com.portfello.data.db.entity.PortfolioSnapshot
import com.portfello.data.repository.AssetRepository
import com.portfello.data.repository.PriceRepository
import com.portfello.domain.AssetValuation
import com.portfello.domain.BullionValuator
import com.portfello.domain.ValuationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChartPoint(val timestamp: Long, val value: Double)

data class DashboardState(
    val totalValue: Double = 0.0,
    val change24hPct: Double? = null,
    val change7dPct: Double? = null,
    val baseCurrency: String = "PLN",
    val allocation: Map<AssetType, Double> = emptyMap(),
    val valuations: List<AssetValuation> = emptyList(),
    val chartData: List<ChartPoint> = emptyList(),
    val selectedRange: Int = 30,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val assetRepo: AssetRepository,
    private val valuationEngine: ValuationEngine,
    private val priceRepo: PriceRepository,
    private val bullionValuator: BullionValuator,
    private val snapshotDao: PortfolioSnapshotDao,
    private val prefs: AppPrefs
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState(baseCurrency = prefs.baseCurrency))
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        if (_state.value.isRefreshing) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isRefreshing = true)
            val assets = assetRepo.getAllAssets().first()
            val baseCurrency = prefs.baseCurrency
            val valuations = assets.map { valuationEngine.valuate(it, baseCurrency) }
            val total = valuations.sumOf { it.totalValue }
            val allocation = valuations
                .groupBy { it.asset.type }
                .mapValues { (_, vs) -> vs.sumOf { it.totalValue } }

            // keep the snapshot history fresh even without background sync (throttled to 1/h)
            if (total > 0) {
                val latest = snapshotDao.getLatest(baseCurrency)
                if (latest == null || System.currentTimeMillis() - latest.timestamp > 3_600_000) {
                    snapshotDao.insert(PortfolioSnapshot(
                        timestamp = System.currentTimeMillis(),
                        totalValue = total,
                        baseCurrency = baseCurrency
                    ))
                }
            }

            val chartData = try {
                buildChartData(_state.value.selectedRange)
            } catch (_: Exception) {
                emptyList()
            }

            _state.value = DashboardState(
                totalValue = total,
                change24hPct = totalChangePct(total, baseCurrency, 24 * 3_600_000L),
                change7dPct = totalChangePct(total, baseCurrency, 7 * 24 * 3_600_000L),
                baseCurrency = baseCurrency,
                allocation = allocation,
                valuations = valuations,
                chartData = chartData,
                selectedRange = _state.value.selectedRange,
                isLoading = false,
                isRefreshing = false
            )
        }
    }

    fun selectRange(days: Int) {
        _state.value = _state.value.copy(selectedRange = days)
        viewModelScope.launch {
            val chartData = try {
                buildChartData(days)
            } catch (_: Exception) {
                emptyList()
            }
            _state.value = _state.value.copy(chartData = chartData)
        }
    }

    private suspend fun totalChangePct(total: Double, baseCurrency: String, agoMs: Long): Double? {
        if (total <= 0) return null
        val old = snapshotDao.getLatestBefore(System.currentTimeMillis() - agoMs, baseCurrency) ?: return null
        // reject snapshots more than twice the window old — a "24h" badge based on last month is a lie
        if (old.timestamp < System.currentTimeMillis() - 2 * agoMs) return null
        if (old.totalValue == 0.0) return null
        return (total - old.totalValue) / old.totalValue * 100
    }

    private suspend fun buildChartData(days: Int): List<ChartPoint> {
        val dayMs = 24L * 3600 * 1000
        val since = System.currentTimeMillis() - days * dayMs
        // one point per day (last snapshot wins) — the chart's x-axis is day-resolution
        val snapshots = snapshotDao.getHistoryList(since, prefs.baseCurrency)
            .associateBy { it.timestamp / dayMs }
            .values.sortedBy { it.timestamp }
        if (snapshots.size >= 2) {
            return snapshots.map { ChartPoint(it.timestamp, it.totalValue) }
        }
        // not enough stored snapshots yet — rebuild from per-asset price history
        return buildChartDataFromHistory(days)
    }

    private suspend fun buildChartDataFromHistory(days: Int): List<ChartPoint> {
        val assets = assetRepo.getAllAssets().first()
        val baseCurrency = prefs.baseCurrency
        val dayMs = 24L * 3600 * 1000
        val cutoff = System.currentTimeMillis() - days * dayMs

        data class AssetHistory(val qty: Double, val prices: java.util.TreeMap<Long, Double>)

        val histories = assets.mapNotNull { asset ->
            if (asset.type == AssetType.BOND_RETAIL || asset.type == AssetType.MANUAL) return@mapNotNull null
            val holdings = assetRepo.getHoldings(asset.id).first()
            val qty = holdings.sumOf { it.quantity }
            if (qty <= 0) return@mapNotNull null

            val history = try {
                if (asset.type == AssetType.METAL_BULLION) {
                    // bullion has no ticker — scale the metal spot history to per-unit value
                    val details = assetRepo.getBullionDetails(asset.id) ?: return@mapNotNull null
                    bullionValuator.unitValueHistory(details, days).getOrNull()
                } else {
                    val ticker = asset.tickerOrId ?: return@mapNotNull null
                    priceRepo.getHistory(asset.type, ticker, baseCurrency, days).getOrNull()
                }
            } catch (_: Exception) { null }
                ?: return@mapNotNull null

            val byDay = java.util.TreeMap<Long, Double>()
            for (p in history) {
                if (p.timestamp >= cutoff) {
                    byDay[(p.timestamp / dayMs) * dayMs] = p.price
                }
            }
            if (byDay.isEmpty()) return@mapNotNull null
            AssetHistory(qty, byDay)
        }

        if (histories.isEmpty()) return emptyList()

        val allDays = java.util.TreeSet<Long>()
        histories.forEach { allDays.addAll(it.prices.keys) }

        return allDays.map { day ->
            val total = histories.sumOf { h ->
                val price = h.prices.floorEntry(day)?.value ?: 0.0
                h.qty * price
            }
            ChartPoint(day, total)
        }
    }
}
