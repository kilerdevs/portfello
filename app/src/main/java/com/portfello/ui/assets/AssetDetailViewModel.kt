package com.portfello.ui.assets

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfello.R
import com.portfello.data.AppPrefs
import com.portfello.data.db.entity.Asset
import com.portfello.data.db.entity.AssetHolding
import com.portfello.data.db.entity.AssetType
import com.portfello.data.repository.AssetRepository
import com.portfello.data.repository.HistoryPoint
import com.portfello.data.repository.PriceRepository
import com.portfello.domain.AssetValuation
import com.portfello.domain.BullionValuator
import com.portfello.domain.ValuationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AssetDetailState(
    val valuation: AssetValuation? = null,
    val holdings: List<AssetHolding> = emptyList(),
    val priceHistory: List<HistoryPoint> = emptyList(),
    val selectedRange: Int = 30, // days
    val isLoading: Boolean = true,
    val historyError: String? = null
)

@HiltViewModel
class AssetDetailViewModel @Inject constructor(
    private val assetRepo: AssetRepository,
    private val priceRepo: PriceRepository,
    private val valuationEngine: ValuationEngine,
    private val bullionValuator: BullionValuator,
    private val prefs: AppPrefs,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(AssetDetailState())
    val state: StateFlow<AssetDetailState> = _state.asStateFlow()

    fun load(assetId: Long) {
        viewModelScope.launch {
            val asset = assetRepo.getById(assetId) ?: return@launch
            val valuation = valuationEngine.valuate(asset, prefs.baseCurrency)
            val holdings = assetRepo.getHoldings(assetId).first()
            _state.value = AssetDetailState(
                valuation = valuation,
                holdings = holdings,
                isLoading = false
            )
            loadHistory(asset, _state.value.selectedRange)
        }
    }

    fun selectRange(days: Int) {
        _state.value = _state.value.copy(selectedRange = days)
        val asset = _state.value.valuation?.asset ?: return
        viewModelScope.launch { loadHistory(asset, days) }
    }

    fun deleteAsset(onDone: () -> Unit) {
        val asset = _state.value.valuation?.asset ?: return
        viewModelScope.launch {
            assetRepo.deleteAsset(asset)
            onDone()
        }
    }

    private suspend fun loadHistory(asset: Asset, days: Int) {
        val result = if (asset.type == AssetType.METAL_BULLION) {
            // bullion has no ticker — chart the per-unit value (spot x weight x purity + premium)
            val details = assetRepo.getBullionDetails(asset.id) ?: return
            bullionValuator.unitValueHistory(details, days)
        } else {
            val ticker = asset.tickerOrId ?: return
            priceRepo.getHistory(asset.type, ticker, asset.currency, days)
        }
        result
            .onSuccess { history ->
                _state.value = _state.value.copy(priceHistory = history, historyError = null)
            }
            .onFailure { e ->
                _state.value = _state.value.copy(historyError = e.message ?: context.getString(R.string.history_fetch_error))
            }
    }
}
