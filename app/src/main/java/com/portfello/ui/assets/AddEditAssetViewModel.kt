package com.portfello.ui.assets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfello.data.bullion.BullionCoinCatalog
import com.portfello.data.bullion.BullionCoinSpec
import com.portfello.data.db.entity.Asset
import com.portfello.data.db.entity.AssetHolding
import com.portfello.data.db.entity.AssetType
import com.portfello.data.db.entity.BondRetailDetails
import com.portfello.data.db.entity.BullionDetails
import com.portfello.data.db.entity.PriceSnapshot
import com.portfello.data.db.dao.PriceSnapshotDao
import com.portfello.data.repository.AssetRepository
import com.portfello.data.repository.PriceRepository
import com.portfello.data.repository.SearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditState(
    val isEdit: Boolean = false,
    val assetId: Long? = null,
    val holdingId: Long? = null,
    val type: AssetType = AssetType.STOCK,
    val name: String = "",
    val tickerOrId: String = "",
    val currency: String = "PLN",
    val quantity: String = "1",
    val purchasePrice: String = "",
    val notes: String = "",
    // Bond retail
    val bondNominal: String = "100",
    val bondSeries: String = "",
    val bondRate: String = "",
    val bondCapType: String = "ANNUAL",
    // Bullion
    val selectedCoin: BullionCoinSpec? = null,
    val metalType: String = "Au",
    val bullionWeight: String = "",
    val bullionPurity: String = "",
    val bullionPremium: String = "0",
    // Manual
    val manualValue: String = "",
    // Search
    val searchResults: List<SearchResult> = emptyList(),
    val searchQuery: String = "",
    // State
    val isSaving: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AddEditAssetViewModel @Inject constructor(
    private val assetRepo: AssetRepository,
    private val priceRepo: PriceRepository,
    private val priceSnapshotDao: PriceSnapshotDao
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditState())
    val state: StateFlow<AddEditState> = _state.asStateFlow()

    fun loadForEdit(assetId: Long) {
        viewModelScope.launch {
            val asset = assetRepo.getById(assetId) ?: return@launch
            val holding = assetRepo.getHoldings(assetId).first().firstOrNull()
            _state.value = _state.value.copy(
                isEdit = true,
                assetId = assetId,
                holdingId = holding?.id,
                type = asset.type,
                name = asset.name,
                tickerOrId = asset.tickerOrId ?: "",
                currency = asset.currency,
                quantity = holding?.quantity?.toString() ?: "1",
                purchasePrice = holding?.purchasePrice?.toString() ?: "",
                notes = asset.notes ?: ""
            )
            when (asset.type) {
                AssetType.BOND_RETAIL -> {
                    assetRepo.getBondDetails(assetId)?.let { d ->
                        _state.value = _state.value.copy(
                            bondNominal = d.nominal.toString(),
                            bondSeries = d.series,
                            bondRate = d.interestRate.toString(),
                            bondCapType = d.capitalizationType
                        )
                    }
                }
                AssetType.METAL_BULLION -> {
                    assetRepo.getBullionDetails(assetId)?.let { d ->
                        _state.value = _state.value.copy(
                            metalType = d.metal,
                            bullionWeight = d.totalWeightGrams.toString(),
                            bullionPurity = d.purity.toString(),
                            bullionPremium = d.premiumPln.toString()
                        )
                    }
                }
                else -> {}
            }
        }
    }

    fun update(fn: AddEditState.() -> AddEditState) {
        _state.value = _state.value.fn()
    }

    fun selectCoin(coin: BullionCoinSpec) {
        _state.value = _state.value.copy(
            selectedCoin = coin,
            name = coin.name,
            metalType = coin.metal,
            bullionWeight = coin.totalWeightGrams.toString(),
            bullionPurity = coin.purity.toString()
        )
    }

    fun search(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        if (query.length < 2) return
        viewModelScope.launch {
            priceRepo.search(_state.value.type, query).onSuccess { results ->
                _state.value = _state.value.copy(searchResults = results)
            }
        }
    }

    fun selectSearchResult(result: SearchResult) {
        _state.value = _state.value.copy(
            tickerOrId = result.id,
            name = result.name.ifEmpty { result.symbol },
            searchResults = emptyList(),
            searchQuery = ""
        )
    }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.value = s.copy(error = "Nazwa jest wymagana")
            return
        }
        _state.value = s.copy(isSaving = true, error = null)
        viewModelScope.launch {
            try {
                val qty = s.quantity.toDoubleOrNull() ?: 1.0
                val price = s.purchasePrice.toDoubleOrNull()
                val id: Long
                if (s.assetId != null) {
                    val original = assetRepo.getById(s.assetId) ?: throw Exception("Aktywo nie istnieje")
                    id = s.assetId
                    assetRepo.updateAsset(
                        original.copy(
                            type = s.type,
                            name = s.name.trim(),
                            tickerOrId = s.tickerOrId.ifBlank { null },
                            currency = s.currency,
                            notes = s.notes.ifBlank { null }
                        )
                    )
                    val holding = assetRepo.getHoldings(id).first().firstOrNull { it.id == s.holdingId }
                    if (holding != null) {
                        assetRepo.updateHolding(holding.copy(quantity = qty, purchasePrice = price))
                    } else {
                        assetRepo.addHolding(
                            AssetHolding(assetId = id, quantity = qty, purchasePrice = price, purchaseDate = System.currentTimeMillis())
                        )
                    }
                } else {
                    id = assetRepo.addAsset(
                        Asset(
                            type = s.type,
                            name = s.name.trim(),
                            tickerOrId = s.tickerOrId.ifBlank { null },
                            currency = s.currency,
                            notes = s.notes.ifBlank { null }
                        )
                    )
                    assetRepo.addHolding(
                        AssetHolding(
                            assetId = id,
                            quantity = qty,
                            purchasePrice = price,
                            purchaseDate = System.currentTimeMillis()
                        )
                    )
                }

                when (s.type) {
                    AssetType.BOND_RETAIL -> {
                        assetRepo.saveBondDetails(
                            BondRetailDetails(
                                assetId = id,
                                nominal = s.bondNominal.toDoubleOrNull() ?: 100.0,
                                series = s.bondSeries,
                                interestRate = s.bondRate.toDoubleOrNull() ?: 0.0,
                                rateUpdatedAt = System.currentTimeMillis(),
                                capitalizationType = s.bondCapType
                            )
                        )
                    }
                    AssetType.METAL_BULLION -> {
                        assetRepo.saveBullionDetails(
                            BullionDetails(
                                assetId = id,
                                metal = s.metalType,
                                totalWeightGrams = s.bullionWeight.toDoubleOrNull() ?: 31.11,
                                purity = s.bullionPurity.toDoubleOrNull() ?: 0.9999,
                                premiumPln = s.bullionPremium.toDoubleOrNull() ?: 0.0
                            )
                        )
                    }
                    AssetType.MANUAL -> {
                        val manualVal = s.manualValue.toDoubleOrNull()
                        if (manualVal != null) {
                            priceSnapshotDao.insert(
                                PriceSnapshot(
                                    assetId = id,
                                    timestamp = System.currentTimeMillis(),
                                    price = manualVal,
                                    currency = s.currency,
                                    source = "manual"
                                )
                            )
                        }
                    }
                    else -> {}
                }
                onDone()
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, error = e.message)
            }
        }
    }
}
