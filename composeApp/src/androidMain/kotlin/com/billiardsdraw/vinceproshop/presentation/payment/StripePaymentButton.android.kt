package com.billiardsdraw.vinceproshop.presentation.payment

import androidx.activity.ComponentActivity
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.billiardsdraw.vinceproshop.core.formatEuro
import com.billiardsdraw.vinceproshop.core.localStripeMerchantDisplayName
import com.billiardsdraw.vinceproshop.core.localStripePublishableKey
import com.billiardsdraw.vinceproshop.domain.model.CheckoutCustomerInfo
import com.billiardsdraw.vinceproshop.domain.model.paymentIntentIdFromClientSecret
import com.billiardsdraw.vinceproshop.presentation.common.tr
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import com.stripe.android.paymentsheet.rememberPaymentSheet

@Suppress("ModifierDefaultValue")
@Composable
actual fun StripePaymentButton(
    clientSecret: String,
    customerInfo: CheckoutCustomerInfo,
    amount: Double,
    enabled: Boolean,
    onLaunch: () -> Unit,
    onResult: (StripePaymentResult) -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val publishableKey = localStripePublishableKey().trim()
    val merchantDisplayName = localStripeMerchantDisplayName().trim().ifBlank { "Vince Pro Shop" }

    LaunchedEffect(publishableKey) {
        if (publishableKey.isNotBlank()) {
            PaymentConfiguration.init(context, publishableKey)
        }
    }

    val callback = PaymentSheetResultCallback { result ->
        when (result) {
            is PaymentSheetResult.Completed ->
                onResult(StripePaymentResult.Completed(paymentIntentIdFromClientSecret(clientSecret)))

            is PaymentSheetResult.Canceled ->
                onResult(StripePaymentResult.Canceled)

            is PaymentSheetResult.Failed ->
                onResult(
                    StripePaymentResult.Failed(
                        result.error.localizedMessage ?: "Stripe payment failed."
                    )
                )
        }
    }

    val paymentSheet = remember {
        PaymentSheet.Builder(callback).build(activity)
    }

    Button(
        onClick = {
            if (publishableKey.isBlank()) {
                onResult(StripePaymentResult.Failed("Missing VINCE_STRIPE_PUBLISHABLE_KEY in local.properties."))
                return@Button
            }
            onLaunch()
            paymentSheet.presentWithPaymentIntent(
                clientSecret,
                PaymentSheet.Configuration(
                    merchantDisplayName = merchantDisplayName,
                    allowsDelayedPaymentMethods = true,
                )
            )
        },
        enabled = enabled,
        modifier = modifier,
    ) {
        Text(
            text = tr(
                "Pay now (${formatEuro(amount)})",
                "Pagar ahora (${formatEuro(amount)})",
            )
        )
    }
}
