package com.billiardsdraw.vinceproshop.presentation.payment

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.billiardsdraw.vinceproshop.domain.model.CheckoutCustomerInfo

sealed interface StripePaymentResult {
    data class Completed(val paymentIntentId: String) : StripePaymentResult
    data object Canceled : StripePaymentResult
    data class Failed(val message: String) : StripePaymentResult
}

@Composable
expect fun StripePaymentButton(
    clientSecret: String,
    customerInfo: CheckoutCustomerInfo,
    amount: Double,
    enabled: Boolean,
    onLaunch: () -> Unit,
    onResult: (StripePaymentResult) -> Unit,
    modifier: Modifier = Modifier,
)
