package com.billiardsdraw.vinceproshop.presentation.admin.manage.crosssell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billiardsdraw.vinceproshop.core.DispatchersProvider
import com.billiardsdraw.vinceproshop.data.remote.AdminApi
import com.billiardsdraw.vinceproshop.data.remote.AdminCrossSellRuleDto
import com.billiardsdraw.vinceproshop.data.remote.AdminCrossSellRuleUpsertRequestDto
import com.billiardsdraw.vinceproshop.data.remote.CatalogApi
import com.billiardsdraw.vinceproshop.data.remote.CategoryDto
import com.billiardsdraw.vinceproshop.data.remote.ProductDto
import com.billiardsdraw.vinceproshop.presentation.common.tr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminManageCrossSellUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val rules: List<AdminCrossSellRuleDto> = emptyList(),
    val products: List<ProductDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
)

class AdminManageCrossSellViewModel(
    private val adminApi: AdminApi,
    private val catalogApi: CatalogApi,
    private val dispatchers: DispatchersProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(AdminManageCrossSellUiState())
    val state: StateFlow<AdminManageCrossSellUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun setError(message: String) {
        _state.value = _state.value.copy(error = message)
    }

    fun refresh() {
        viewModelScope.launch(dispatchers.io) {
            _state.value = _state.value.copy(isLoading = true)
            runCatching {
                Triple(adminApi.crossSellRules("all"), catalogApi.getProducts(), catalogApi.getCategories())
            }.onSuccess { (rules, products, categories) ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = null,
                    rules = rules,
                    products = products,
                    categories = categories,
                )
            }.onFailure { throwable ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = throwable.message ?: tr(
                        "Could not load cross-sell rules",
                        "No se pudieron cargar reglas cross-sell",
                    ),
                )
            }
        }
    }

    fun deleteRule(ruleId: Int) {
        viewModelScope.launch(dispatchers.io) {
            runCatching { adminApi.deleteCrossSellRule(ruleId) }
                .onFailure { throwable ->
                    _state.value = _state.value.copy(
                        error = throwable.message ?: tr("Delete failed", "No se pudo eliminar"),
                    )
                }
            refresh()
        }
    }

    fun recomputeAnalytics(productId: Int) {
        viewModelScope.launch(dispatchers.io) {
            runCatching { adminApi.recomputeCrossSellAnalytics(productId) }
                .onFailure { throwable ->
                    _state.value = _state.value.copy(
                        error = throwable.message ?: tr("Recompute failed", "No se pudo recalcular"),
                    )
                }
            refresh()
        }
    }

    fun saveRule(
        ruleId: Int,
        payload: AdminCrossSellRuleUpsertRequestDto,
        analyticsControlProductId: Int?,
        analyticsLocked: Boolean,
    ) {
        _state.value = _state.value.copy(isSaving = true)
        viewModelScope.launch(dispatchers.io) {
            val result = runCatching {
                if (ruleId == 0) {
                    adminApi.createCrossSellRule(payload)
                } else {
                    adminApi.updateCrossSellRule(ruleId, payload)
                }
                if (payload.sourceType == "analytics" && analyticsControlProductId != null) {
                    adminApi.updateCrossSellAnalyticsControl(
                        productId = analyticsControlProductId,
                        isLocked = analyticsLocked,
                    )
                }
            }
            if (result.isFailure) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = result.exceptionOrNull()?.message ?: tr("Save failed", "No se pudo guardar"),
                )
                return@launch
            }
            _state.value = _state.value.copy(isSaving = false, error = null)
            refresh()
        }
    }
}
