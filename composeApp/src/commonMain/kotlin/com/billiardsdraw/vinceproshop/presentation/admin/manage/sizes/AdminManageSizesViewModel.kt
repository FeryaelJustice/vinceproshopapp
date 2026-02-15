package com.billiardsdraw.vinceproshop.presentation.admin.manage.sizes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billiardsdraw.vinceproshop.core.DispatchersProvider
import com.billiardsdraw.vinceproshop.data.remote.AdminApi
import com.billiardsdraw.vinceproshop.data.remote.AdminSizeDto
import com.billiardsdraw.vinceproshop.presentation.common.tr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminManageSizesUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val sizes: List<AdminSizeDto> = emptyList(),
    val editing: AdminSizeDto? = null,
    val draftName: String = "",
)

class AdminManageSizesViewModel(
    private val adminApi: AdminApi,
    private val dispatchers: DispatchersProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(AdminManageSizesUiState())
    val state: StateFlow<AdminManageSizesUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(dispatchers.io) {
            _state.value = _state.value.copy(isLoading = true)
            runCatching { adminApi.sizes() }
                .onSuccess { sizes ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = null,
                        sizes = sizes,
                    )
                }
                .onFailure { throwable ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = throwable.message ?: tr("Could not load sizes", "No se pudieron cargar tallas"),
                    )
                }
        }
    }

    fun startAdd() {
        _state.value = _state.value.copy(editing = AdminSizeDto(id = 0, name = ""), draftName = "")
    }

    fun startEdit(size: AdminSizeDto) {
        _state.value = _state.value.copy(editing = size, draftName = size.name)
    }

    fun dismissEditor() {
        _state.value = _state.value.copy(editing = null, draftName = "")
    }

    fun updateDraftName(value: String) {
        _state.value = _state.value.copy(draftName = value)
    }

    fun deleteSize(sizeId: Int) {
        viewModelScope.launch(dispatchers.io) {
            runCatching { adminApi.deleteSize(sizeId) }
                .onFailure { throwable ->
                    _state.value = _state.value.copy(
                        error = throwable.message ?: tr("Delete failed", "No se pudo eliminar"),
                    )
                }
            refresh()
        }
    }

    fun saveDraft() {
        val current = _state.value
        val target = current.editing ?: return
        val cleanName = current.draftName.trim()
        if (cleanName.isBlank()) {
            _state.value = current.copy(error = tr("Name is required", "Nombre requerido"))
            return
        }
        _state.value = current.copy(isSaving = true)
        viewModelScope.launch(dispatchers.io) {
            val result = runCatching {
                if (target.id == 0) {
                    adminApi.createSize(cleanName)
                } else {
                    adminApi.updateSize(target.id, cleanName)
                }
            }
            if (result.isFailure) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = result.exceptionOrNull()?.message ?: tr("Save failed", "No se pudo guardar"),
                )
                return@launch
            }
            _state.value = _state.value.copy(isSaving = false, editing = null, draftName = "", error = null)
            refresh()
        }
    }
}

