package com.billiardsdraw.vinceproshop.presentation.admin.manage.featured

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billiardsdraw.vinceproshop.core.DispatchersProvider
import com.billiardsdraw.vinceproshop.data.remote.AdminApi
import com.billiardsdraw.vinceproshop.data.remote.AdminFeaturedUpsertRequestDto
import com.billiardsdraw.vinceproshop.data.remote.CatalogApi
import com.billiardsdraw.vinceproshop.data.remote.CategoryDto
import com.billiardsdraw.vinceproshop.data.remote.FeaturedSlideDto
import com.billiardsdraw.vinceproshop.data.remote.ProductDto
import com.billiardsdraw.vinceproshop.presentation.common.tr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminManageFeaturedUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val featured: List<FeaturedSlideDto> = emptyList(),
    val products: List<ProductDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
)

class AdminManageFeaturedViewModel(
    private val adminApi: AdminApi,
    private val catalogApi: CatalogApi,
    private val dispatchers: DispatchersProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(AdminManageFeaturedUiState())
    val state: StateFlow<AdminManageFeaturedUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(dispatchers.io) {
            _state.value = _state.value.copy(isLoading = true)
            runCatching {
                Triple(adminApi.featured(), catalogApi.getProducts(), adminApi.categories())
            }.onSuccess { (featured, products, categories) ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = null,
                    featured = featured,
                    products = products,
                    categories = categories,
                )
            }.onFailure { throwable ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = throwable.message ?: tr("Could not load featured items", "No se pudo cargar destacados"),
                )
            }
        }
    }

    fun deleteFeatured(id: Int) {
        viewModelScope.launch(dispatchers.io) {
            runCatching { adminApi.deleteFeatured(id) }
                .onFailure { throwable ->
                    _state.value = _state.value.copy(
                        error = throwable.message ?: tr("Delete failed", "No se pudo eliminar"),
                    )
                }
            refresh()
        }
    }

    fun saveFeatured(featuredId: Int, payload: AdminFeaturedUpsertRequestDto) {
        _state.value = _state.value.copy(isSaving = true)
        viewModelScope.launch(dispatchers.io) {
            val result = runCatching {
                if (featuredId == 0) {
                    adminApi.createFeatured(payload)
                } else {
                    adminApi.updateFeatured(featuredId, payload)
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

