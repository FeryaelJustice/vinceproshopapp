package com.billiardsdraw.vinceproshop.presentation.admin.outofstock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billiardsdraw.vinceproshop.core.DispatchersProvider
import com.billiardsdraw.vinceproshop.data.remote.AdminApi
import com.billiardsdraw.vinceproshop.data.remote.AdminStockInterestRequestDto
import com.billiardsdraw.vinceproshop.presentation.common.tr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminOutOfStockInterestedUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val rows: List<AdminStockInterestRequestDto> = emptyList(),
)

class AdminOutOfStockInterestedViewModel(
    private val adminApi: AdminApi,
    private val dispatchers: DispatchersProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(AdminOutOfStockInterestedUiState())
    val state: StateFlow<AdminOutOfStockInterestedUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(dispatchers.io) {
            _state.value = _state.value.copy(isLoading = true)
            runCatching { adminApi.stockInterestRequests() }
                .onSuccess { rows ->
                    _state.value =
                        _state.value.copy(
                            isLoading = false,
                            error = null,
                            rows = rows,
                        )
                }.onFailure { throwable ->
                    _state.value =
                        _state.value.copy(
                            isLoading = false,
                            error =
                                throwable.message ?: tr(
                                    "Could not load out-of-stock requests",
                                    "No se pudo cargar solicitudes sin stock",
                                ),
                        )
                }
        }
    }
}
