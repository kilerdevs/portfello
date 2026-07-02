package com.portfello.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfello.data.AppPrefs
import com.portfello.data.db.entity.AssetType
import com.portfello.data.repository.AssetRepository
import com.portfello.data.repository.PriceRepository
import com.portfello.domain.AssetValuation
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

            val chartData = try {
                buildChartData(_state.value.selectedRange)
            } catch (_: Exception) {
                emptyList()
            }

            _state.value = DashboardState(
                totalValue = total,
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

    private suspend fun buildChartData(days: Int): List<ChartPoint> {
        val assets = assetRepo.getAllAssets().first()
        val baseCurrency = prefs.baseCurrency
        val dayMs = 24L * 3600 * 1000
        val cutoff = System.currentTimeMillis() - days * dayMs

        data class AssetHistory(val qty: Double, val prices: java.util.TreeMap<Long, Double>)

        val histories = assets.mapNotNull { asset ->
            val ticker = asset.tickerOrId ?: return@mapNotNull null
            if (asset.type == AssetType.BOND_RETAIL || asset.type == AssetType.MANUAL) return@mapNotNull null
            val holdings = assetRepo.getHoldings(asset.id).first()
            val qty = holdings.sumOf { it.quantity }
            if (qty <= 0) return@mapNotNull null

            val history = try {
                priceRepo.getHistory(asset.type, ticker, baseCurrency, days).getOrNull()
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
