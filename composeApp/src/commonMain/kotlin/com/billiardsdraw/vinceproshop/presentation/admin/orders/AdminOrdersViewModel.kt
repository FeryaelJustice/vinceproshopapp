package com.billiardsdraw.vinceproshop.presentation.admin.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billiardsdraw.vinceproshop.core.DispatchersProvider
import com.billiardsdraw.vinceproshop.data.remote.AdminApi
import com.billiardsdraw.vinceproshop.data.remote.OrderDto
import com.billiardsdraw.vinceproshop.presentation.common.tr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminOrdersUiState(
    val isLoading: Boolean = true,
    val isUpdating: Boolean = false,
    val error: String? = null,
    val orders: List<OrderDto> = emptyList(),
)

class AdminOrdersViewModel(
    private val adminApi: AdminApi,
    private val dispatchers: DispatchersProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(AdminOrdersUiState())
    val state: StateFlow<AdminOrdersUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(dispatchers.io) {
            _state.value = _state.value.copy(isLoading = true)
            runCatching { adminApi.orders() }
                .onSuccess { orders ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = null,
                        orders = orders,
                    )
                }
                .onFailure { throwable ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = throwable.message ?: tr("Could not load orders", "No se pudieron cargar pedidos"),
                    )
                }
        }
    }

    fun updateOrderStatus(orderId: Int, nextStatus: String) {
        val previous = _state.value.orders
        _state.value = _state.value.copy(
            isUpdating = true,
            orders = previous.map { if (it.id == orderId) it.copy(status = nextStatus) else it },
        )
        viewModelScope.launch(dispatchers.io) {
            runCatching { adminApi.updateOrderStatus(orderId, nextStatus) }
                .onFailure { throwable ->
                    _state.value = _state.value.copy(
                        isUpdating = false,
                        error = throwable.message ?: tr("Status update failed", "No se pudo actualizar estado"),
                        orders = previous,
                    )
                    return@launch
                }
            _state.value = _state.value.copy(isUpdating = false)
            refresh()
        }
    }
}

