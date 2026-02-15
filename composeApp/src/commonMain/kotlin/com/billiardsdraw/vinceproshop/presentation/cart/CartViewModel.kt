package com.billiardsdraw.vinceproshop.presentation.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billiardsdraw.vinceproshop.core.DispatchersProvider
import com.billiardsdraw.vinceproshop.domain.model.CartItem
import com.billiardsdraw.vinceproshop.domain.model.CheckoutCustomerInfo
import com.billiardsdraw.vinceproshop.domain.model.CheckoutLineItem
import com.billiardsdraw.vinceproshop.domain.model.CheckoutPaymentIntentPayload
import com.billiardsdraw.vinceproshop.domain.model.fullAddress
import com.billiardsdraw.vinceproshop.domain.model.isValid
import com.billiardsdraw.vinceproshop.domain.model.paymentIntentIdFromClientSecret
import com.billiardsdraw.vinceproshop.domain.usecase.ClearCartUseCase
import com.billiardsdraw.vinceproshop.domain.usecase.CreatePaymentIntentUseCase
import com.billiardsdraw.vinceproshop.domain.usecase.ObserveCartUseCase
import com.billiardsdraw.vinceproshop.domain.usecase.RemoveCartItemUseCase
import com.billiardsdraw.vinceproshop.domain.usecase.UpdateCartQuantityUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class CheckoutStep {
    Shipping,
    Payment,
    Completed,
}

data class CartUiState(
    val isLoading: Boolean = true,
    val items: List<CartItem> = emptyList(),
    val subtotal: Double = 0.0,
    val totalItems: Int = 0,
    val checkoutStep: CheckoutStep = CheckoutStep.Shipping,
    val customerInfo: CheckoutCustomerInfo = CheckoutCustomerInfo(),
    val isCreatingPaymentIntent: Boolean = false,
    val isProcessingPayment: Boolean = false,
    val paymentClientSecret: String? = null,
    val paymentIntentId: String = "",
    val paymentAmount: Double = 0.0,
    val paymentCartFingerprint: String? = null,
    val checkoutError: String? = null,
    val checkoutMessage: String? = null,
)

class CartViewModel(
    observeCart: ObserveCartUseCase,
    private val updateCartQuantity: UpdateCartQuantityUseCase,
    private val removeCartItem: RemoveCartItemUseCase,
    private val clearCart: ClearCartUseCase,
    private val createPaymentIntent: CreatePaymentIntentUseCase,
    private val dispatchers: DispatchersProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(CartUiState())
    val state: StateFlow<CartUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch(dispatchers.main) {
            observeCart().collect { snapshot ->
                val fingerprint = cartFingerprint(snapshot.items)
                val current = _state.value
                var next = current.copy(
                    isLoading = false,
                    items = snapshot.items,
                    subtotal = snapshot.subtotal,
                    totalItems = snapshot.totalItems,
                )

                if (current.checkoutStep == CheckoutStep.Payment &&
                    current.paymentCartFingerprint != null &&
                    current.paymentCartFingerprint != fingerprint
                ) {
                    next = next.copy(
                        checkoutStep = CheckoutStep.Shipping,
                        isCreatingPaymentIntent = false,
                        isProcessingPayment = false,
                        paymentClientSecret = null,
                        paymentIntentId = "",
                        paymentAmount = 0.0,
                        paymentCartFingerprint = null,
                        checkoutError = "Cart changed. Review your order and continue to payment again.",
                        checkoutMessage = null,
                    )
                }

                _state.value = next
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
            _state.value = _state.value.copy(
                checkoutStep = CheckoutStep.Shipping,
                isCreatingPaymentIntent = false,
                isProcessingPayment = false,
                paymentClientSecret = null,
                paymentIntentId = "",
                paymentAmount = 0.0,
                paymentCartFingerprint = null,
                checkoutError = null,
                checkoutMessage = null,
            )
        }
    }

    fun updateCustomerInfo(info: CheckoutCustomerInfo) {
        _state.value = _state.value.copy(
            customerInfo = info,
            checkoutError = null,
        )
    }

    fun continueToPayment(languageCode: String) {
        val snapshot = _state.value
        if (snapshot.items.isEmpty()) {
            _state.value = snapshot.copy(
                checkoutError = "Cart is empty.",
                checkoutMessage = null,
            )
            return
        }
        if (!snapshot.customerInfo.isValid()) {
            _state.value = snapshot.copy(
                checkoutError = "Complete all shipping fields before continuing.",
                checkoutMessage = null,
            )
            return
        }

        _state.value = snapshot.copy(
            isCreatingPaymentIntent = true,
            checkoutError = null,
            checkoutMessage = null,
        )

        viewModelScope.launch(dispatchers.io) {
            runCatching {
                createPaymentIntent(
                    CheckoutPaymentIntentPayload(
                        items = snapshot.items.map { item ->
                            CheckoutLineItem(
                                slug = item.slug,
                                size = item.size,
                                quantity = item.quantity,
                            )
                        },
                        currency = "eur",
                        customerEmail = snapshot.customerInfo.email.trim().lowercase(),
                        customerName = snapshot.customerInfo.name.trim(),
                        address = snapshot.customerInfo.fullAddress(),
                        country = snapshot.customerInfo.country.trim(),
                        phone = snapshot.customerInfo.phone.trim(),
                        locale = languageCode,
                    )
                )
            }.onSuccess { intent ->
                if (intent.clientSecret.isBlank()) {
                    _state.value = _state.value.copy(
                        isCreatingPaymentIntent = false,
                        checkoutError = "Backend returned an empty Stripe client secret.",
                        checkoutMessage = null,
                    )
                    return@onSuccess
                }
                _state.value = _state.value.copy(
                    isCreatingPaymentIntent = false,
                    checkoutStep = CheckoutStep.Payment,
                    paymentClientSecret = intent.clientSecret,
                    paymentIntentId = paymentIntentIdFromClientSecret(intent.clientSecret),
                    paymentAmount = intent.amount,
                    paymentCartFingerprint = cartFingerprint(_state.value.items),
                    checkoutError = null,
                    checkoutMessage = null,
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    isCreatingPaymentIntent = false,
                    checkoutError = error.message ?: "Could not initialize Stripe payment.",
                    checkoutMessage = null,
                )
            }
        }
    }

    fun backToShipping() {
        _state.value = _state.value.copy(
            checkoutStep = CheckoutStep.Shipping,
            isCreatingPaymentIntent = false,
            isProcessingPayment = false,
            paymentClientSecret = null,
            paymentIntentId = "",
            paymentAmount = 0.0,
            paymentCartFingerprint = null,
            checkoutError = null,
            checkoutMessage = null,
        )
    }

    fun onPaymentStarted() {
        _state.value = _state.value.copy(
            isProcessingPayment = true,
            checkoutError = null,
            checkoutMessage = null,
        )
    }

    fun onPaymentCanceled() {
        _state.value = _state.value.copy(
            isProcessingPayment = false,
            checkoutMessage = "Payment canceled.",
            checkoutError = null,
        )
    }

    fun onPaymentFailed(message: String) {
        _state.value = _state.value.copy(
            isProcessingPayment = false,
            checkoutError = message.ifBlank { "Payment failed." },
            checkoutMessage = null,
        )
    }

    fun onPaymentCompleted(paymentIntentId: String?) {
        val current = _state.value
        val resolvedPaymentIntentId = paymentIntentId
            ?.takeIf { it.isNotBlank() }
            ?: current.paymentIntentId
            .ifBlank { paymentIntentIdFromClientSecret(current.paymentClientSecret.orEmpty()) }

        viewModelScope.launch(dispatchers.io) {
            runCatching { clearCart() }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isProcessingPayment = false,
                        checkoutError = error.message ?: "Payment succeeded, but cart cleanup failed.",
                        checkoutMessage = null,
                    )
                }
                .onSuccess {
                    _state.value = _state.value.copy(
                        isProcessingPayment = false,
                        checkoutStep = CheckoutStep.Completed,
                        paymentIntentId = resolvedPaymentIntentId,
                        paymentCartFingerprint = null,
                        checkoutError = null,
                        checkoutMessage = "Payment completed.",
                    )
                }
        }
    }

    fun resetCheckoutFlow() {
        _state.value = _state.value.copy(
            checkoutStep = CheckoutStep.Shipping,
            isCreatingPaymentIntent = false,
            isProcessingPayment = false,
            paymentClientSecret = null,
            paymentIntentId = "",
            paymentAmount = 0.0,
            paymentCartFingerprint = null,
            checkoutError = null,
            checkoutMessage = null,
        )
    }

    fun dismissCheckoutFeedback() {
        _state.value = _state.value.copy(
            checkoutError = null,
            checkoutMessage = null,
        )
    }

    private fun cartFingerprint(items: List<CartItem>): String {
        return items.joinToString(separator = "|") { item ->
            "${item.slug}:${item.size}:${item.quantity}:${item.price}"
        }
    }
}
