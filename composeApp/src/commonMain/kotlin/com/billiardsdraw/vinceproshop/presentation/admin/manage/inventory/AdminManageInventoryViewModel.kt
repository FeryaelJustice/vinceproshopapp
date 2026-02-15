package com.billiardsdraw.vinceproshop.presentation.admin.manage.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billiardsdraw.vinceproshop.core.DispatchersProvider
import com.billiardsdraw.vinceproshop.data.remote.AdminApi
import com.billiardsdraw.vinceproshop.data.remote.AdminInventoryProductDto
import com.billiardsdraw.vinceproshop.data.remote.AdminProductUpsertRequestDto
import com.billiardsdraw.vinceproshop.data.remote.AdminSizeDto
import com.billiardsdraw.vinceproshop.data.remote.AdminUploadImage
import com.billiardsdraw.vinceproshop.data.remote.CategoryDto
import com.billiardsdraw.vinceproshop.presentation.common.tr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminManageInventoryUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val products: List<AdminInventoryProductDto> = emptyList(),
    val sizes: List<AdminSizeDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
)

class AdminManageInventoryViewModel(
    private val adminApi: AdminApi,
    private val dispatchers: DispatchersProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(AdminManageInventoryUiState())
    val state: StateFlow<AdminManageInventoryUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(dispatchers.io) {
            _state.value = _state.value.copy(isLoading = true)
            runCatching { Triple(adminApi.inventory(), adminApi.sizes(), adminApi.categories()) }
                .onSuccess { (products, sizes, categories) ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = null,
                        products = products,
                        sizes = sizes,
                        categories = categories,
                    )
                }
                .onFailure { throwable ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = throwable.message ?: tr("Could not load inventory", "No se pudo cargar inventario"),
                    )
                }
        }
    }

    fun discontinueProduct(productId: Int) {
        viewModelScope.launch(dispatchers.io) {
            runCatching { adminApi.discontinueInventoryProduct(productId) }
                .onFailure { throwable ->
                    _state.value = _state.value.copy(
                        error = throwable.message ?: tr("Discontinue failed", "No se pudo descontinuar"),
                    )
                }
            refresh()
        }
    }

    fun saveProduct(
        productId: Int,
        payload: AdminProductUpsertRequestDto,
        uploads: List<AdminUploadImage>,
    ) {
        _state.value = _state.value.copy(isSaving = true)
        viewModelScope.launch(dispatchers.io) {
            val result = runCatching {
                if (productId == 0) {
                    adminApi.createInventoryProduct(payload = payload, images = uploads)
                } else {
                    adminApi.updateInventoryProduct(productId = productId, payload = payload, images = uploads)
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

