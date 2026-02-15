package com.billiardsdraw.vinceproshop.presentation.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billiardsdraw.vinceproshop.core.DispatchersProvider
import com.billiardsdraw.vinceproshop.domain.model.CartItem
import com.billiardsdraw.vinceproshop.domain.model.Product
import com.billiardsdraw.vinceproshop.domain.usecase.AddCartItemUseCase
import com.billiardsdraw.vinceproshop.domain.usecase.ObserveProductDetailUseCase
import com.billiardsdraw.vinceproshop.domain.usecase.RefreshProductUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProductDetailUiState(
    val isLoading: Boolean = true,
    val product: Product? = null,
    val selectedSize: String = "unique",
    val quantity: Int = 1,
    val selectedImageIndex: Int = 0,
    val message: String? = null,
)

class ProductDetailViewModel(
    private val slug: String,
    private val observeProduct: ObserveProductDetailUseCase,
    private val refreshProduct: RefreshProductUseCase,
    private val addCartItem: AddCartItemUseCase,
    private val dispatchers: DispatchersProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(ProductDetailUiState())
    val state: StateFlow<ProductDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch(dispatchers.io) {
            refreshProduct(slug)
        }

        viewModelScope.launch(dispatchers.main) {
            observeProduct(slug).collect { product ->
                val selected = product?.options?.firstOrNull()?.value ?: "unique"
                _state.value = _state.value.copy(
                    isLoading = false,
                    product = product,
                    selectedSize = selected,
                )
            }
        }
    }

    fun onSizeSelected(size: String) {
        _state.value = _state.value.copy(selectedSize = size)
    }

    fun onQuantityChanged(quantity: Int) {
        _state.value = _state.value.copy(quantity = quantity.coerceIn(1, 99))
    }

    fun onImageSelected(index: Int) {
        _state.value = _state.value.copy(selectedImageIndex = index)
    }

    fun dismissMessage() {
        _state.value = _state.value.copy(message = null)
    }

    fun addCurrentSelectionToCart() {
        val snapshot = _state.value
        val product = snapshot.product ?: return
        val option = product.options.firstOrNull { it.value == snapshot.selectedSize }
        val basePrice = option?.price?.takeIf { it > 0.0 } ?: product.price
        val discount = option?.discount ?: product.maxDiscount
        val originalPrice = if (discount > 0.0) basePrice else basePrice

        viewModelScope.launch(dispatchers.io) {
            addCartItem(
                CartItem(
                    slug = product.slug,
                    size = snapshot.selectedSize,
                    quantity = snapshot.quantity,
                    name = product.name,
                    nameEs = product.nameEs,
                    imageUrl = product.imageUrl,
                    price = basePrice,
                    discount = discount,
                    originalPrice = originalPrice,
                )
            )
            _state.value = _state.value.copy(message = "added")
        }
    }
}
