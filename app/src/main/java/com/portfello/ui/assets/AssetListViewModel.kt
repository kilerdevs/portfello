package com.portfello.ui.assets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfello.data.AppPrefs
import com.portfello.data.db.entity.Asset
import com.portfello.data.db.entity.AssetType
import com.portfello.data.repository.AssetRepository
import com.portfello.domain.AssetValuation
import com.portfello.domain.ValuationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AssetListState(
    val groups: Map<AssetType, List<AssetValuation>> = emptyMap(),
    val isLoading: Boolean = true,
    val baseCurrency: String = "PLN"
)

@HiltViewModel
class AssetListViewModel @Inject constructor(
    private val assetRepo: AssetRepository,
    private val valuationEngine: ValuationEngine,
    private val prefs: AppPrefs
) : ViewModel() {

    private val _state = MutableStateFlow(AssetListState(baseCurrency = prefs.baseCurrency))
    val state: StateFlow<AssetListState> = _state.asStateFlow()

    init {
        loadAssets()
    }

    fun loadAssets() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            assetRepo.getAllAssets().collect { assets ->
                val baseCurrency = prefs.baseCurrency
                // Show assets immediately so new items appear without waiting for network
                val placeholder = assets.map {
                    AssetValuation(it, 0.0, baseCurrency, null, null, null)
                }
                _state.value = AssetListState(
                    groups = placeholder.groupBy { it.asset.type },
                    isLoading = false,
                    baseCurrency = baseCurrency
                )
                // Then fill in real valuations
                val valuations = assets.map { valuationEngine.valuate(it, baseCurrency) }
                _state.value = AssetListState(
                    groups = valuations.groupBy { it.asset.type },
                    isLoading = false,
                    baseCurrency = baseCurrency
                )
            }
        }
    }

    fun deleteAsset(asset: Asset) {
        viewModelScope.launch { assetRepo.deleteAsset(asset) }
    }
}
