package com.billiardsdraw.vinceproshop.presentation.admin.manage.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billiardsdraw.vinceproshop.core.DispatchersProvider
import com.billiardsdraw.vinceproshop.data.remote.AdminApi
import com.billiardsdraw.vinceproshop.data.remote.AdminUploadImage
import com.billiardsdraw.vinceproshop.data.remote.CategoryDto
import com.billiardsdraw.vinceproshop.presentation.common.tr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminManageCategoriesUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val categories: List<CategoryDto> = emptyList(),
    val navbarLimit: Int = 6,
    val navbarRootOrder: List<String> = emptyList(),
)

class AdminManageCategoriesViewModel(
    private val adminApi: AdminApi,
    private val dispatchers: DispatchersProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(AdminManageCategoriesUiState())
    val state: StateFlow<AdminManageCategoriesUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(dispatchers.io) {
            _state.value = _state.value.copy(isLoading = true)
            runCatching { Pair(adminApi.categories(), adminApi.navbarConfig()) }
                .onSuccess { (categories, navbar) ->
                    _state.value =
                        _state.value.copy(
                            isLoading = false,
                            error = null,
                            categories = categories,
                            navbarLimit = navbar.rootVisibleLimit,
                            navbarRootOrder = navbar.rootCategories.map { it.id },
                        )
                }.onFailure { throwable ->
                    _state.value =
                        _state.value.copy(
                            isLoading = false,
                            error = throwable.message ?: tr("Could not load categories", "No se pudieron cargar categorias"),
                        )
                }
        }
    }

    fun saveNavbar(
        limit: Int,
        orderedRootIds: List<String>,
    ) {
        viewModelScope.launch(dispatchers.io) {
            runCatching { adminApi.updateNavbarConfig(limit, orderedRootIds) }
                .onFailure { throwable ->
                    _state.value =
                        _state.value.copy(
                            error = throwable.message ?: tr("Navbar update failed", "No se pudo actualizar navbar"),
                        )
                }
            refresh()
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch(dispatchers.io) {
            runCatching { adminApi.deleteCategory(categoryId) }
                .onFailure { throwable ->
                    _state.value =
                        _state.value.copy(
                            error = throwable.message ?: tr("Delete failed", "No se pudo eliminar"),
                        )
                }
            refresh()
        }
    }

    fun saveCategory(
        createMode: Boolean,
        id: String,
        name: String,
        nameEs: String,
        isCue: Boolean,
        parentId: String?,
        image: AdminUploadImage?,
    ) {
        _state.value = _state.value.copy(isSaving = true)
        viewModelScope.launch(dispatchers.io) {
            val result =
                runCatching {
                    if (createMode) {
                        val upload = image ?: throw IllegalArgumentException(tr("Image is required", "Imagen requerida"))
                        adminApi.createCategory(
                            id = id,
                            name = name,
                            nameEs = nameEs,
                            isCue = isCue,
                            parentId = parentId,
                            image = upload,
                        )
                    } else {
                        adminApi.updateCategory(
                            id = id,
                            name = name,
                            nameEs = nameEs,
                            isCue = isCue,
                            parentId = parentId,
                            image = image,
                        )
                    }
                }
            if (result.isFailure) {
                _state.value =
                    _state.value.copy(
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
