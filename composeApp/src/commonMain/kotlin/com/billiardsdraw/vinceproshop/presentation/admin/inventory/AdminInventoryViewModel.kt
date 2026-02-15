package com.billiardsdraw.vinceproshop.presentation.admin.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billiardsdraw.vinceproshop.core.DispatchersProvider
import com.billiardsdraw.vinceproshop.data.remote.AdminApi
import com.billiardsdraw.vinceproshop.data.remote.AdminInventoryProductDto
import com.billiardsdraw.vinceproshop.data.remote.CategoryDto
import com.billiardsdraw.vinceproshop.presentation.common.tr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminInventoryUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val products: List<AdminInventoryProductDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
)

class AdminInventoryViewModel(
    private val adminApi: AdminApi,
    private val dispatchers: DispatchersProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(AdminInventoryUiState())
    val state: StateFlow<AdminInventoryUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(dispatchers.io) {
            _state.value = _state.value.copy(isLoading = true)
            runCatching { Pair(adminApi.inventory(), adminApi.categories()) }
                .onSuccess { (products, categories) ->
                    _state.value =
                        _state.value.copy(
                            isLoading = false,
                            error = null,
                            products = products,
                            categories = categories,
                        )
                }.onFailure { throwable ->
                    _state.value =
                        _state.value.copy(
                            isLoading = false,
                            error =
                                throwable.message
                                    ?: tr("Could not load inventory", "No se pudo cargar inventario"),
                        )
                }
        }
    }
}
