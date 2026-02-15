package com.billiardsdraw.vinceproshop.presentation.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billiardsdraw.vinceproshop.core.DispatchersProvider
import com.billiardsdraw.vinceproshop.domain.model.CartItem
import com.billiardsdraw.vinceproshop.domain.usecase.ClearCartUseCase
import com.billiardsdraw.vinceproshop.domain.usecase.ObserveCartUseCase
import com.billiardsdraw.vinceproshop.domain.usecase.RemoveCartItemUseCase
import com.billiardsdraw.vinceproshop.domain.usecase.UpdateCartQuantityUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CartUiState(
    val isLoading: Boolean = true,
    val items: List<CartItem> = emptyList(),
    val subtotal: Double = 0.0,
    val totalItems: Int = 0,
)

class CartViewModel(
    observeCart: ObserveCartUseCase,
    private val updateCartQuantity: UpdateCartQuantityUseCase,
    private val removeCartItem: RemoveCartItemUseCase,
    private val clearCart: ClearCartUseCase,
    dispatchers: DispatchersProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(CartUiState())
    val state: StateFlow<CartUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch(dispatchers.main) {
            observeCart().collect { snapshot ->
                _state.value = CartUiState(
                    isLoading = false,
                    items = snapshot.items,
                    subtotal = snapshot.subtotal,
                    totalItems = snapshot.totalItems,
                )
            }
        }
    }

    fun updateQuantity(item: CartItem, quantity: Int) {
        viewModelScope.launch {
            updateCartQuantity(item.slug, item.size, quantity)
        }
    }

    fun remove(item: CartItem) {
        viewModelScope.launch {
            removeCartItem(item.slug, item.size)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            clearCart()
        }
    }
}
