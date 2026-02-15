package com.billiardsdraw.vinceproshop.presentation.payment

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.billiardsdraw.vinceproshop.core.formatEuro
import com.billiardsdraw.vinceproshop.domain.model.CheckoutCustomerInfo
import com.billiardsdraw.vinceproshop.domain.model.paymentIntentIdFromClientSecret
import com.billiardsdraw.vinceproshop.presentation.common.tr
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSDictionary
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSString
import kotlin.coroutines.resume

private const val REQUEST_NOTIFICATION = "StripePaymentBridgeRequest"
private const val RESPONSE_NOTIFICATION = "StripePaymentBridgeResponse"

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
    val scope = rememberCoroutineScope()
    val bridge = remember { StripePaymentSheetBridge() }
    var presenting by remember { mutableStateOf(false) }

    Button(
        onClick = {
            if (presenting) return@Button
            presenting = true
            onLaunch()
            scope.launch {
                val result = bridge.presentPaymentSheet(
                    clientSecret = clientSecret,
                    customerInfo = customerInfo,
                )
                presenting = false
                onResult(result)
            }
        },
        enabled = enabled && !presenting,
        modifier = modifier,
    ) {
        Text(
            text = if (presenting) {
                tr("Opening payment sheet...", "Abriendo pasarela de pago...")
            } else {
                tr("Pay now (${formatEuro(amount)})", "Pagar ahora (${formatEuro(amount)})")
            }
        )
    }
}

private class StripePaymentSheetBridge {
    private var nextRequestId: Long = 1L

    suspend fun presentPaymentSheet(
        clientSecret: String,
        customerInfo: CheckoutCustomerInfo,
    ): StripePaymentResult {
        return suspendCancellableCoroutine { continuation ->
            val requestId = nextRequestId++
            var observer: Any? = null
            observer = NSNotificationCenter.defaultCenter.addObserverForName(
                name = RESPONSE_NOTIFICATION,
                `object` = null,
                queue = NSOperationQueue.mainQueue,
            ) { notification ->
                val userInfo = notification?.userInfo as? NSDictionary ?: return@addObserverForName
                val responseId = userInfo.longValue("requestId") ?: return@addObserverForName
                if (responseId != requestId) return@addObserverForName

                observer?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }

                val success = userInfo.boolValue("success") ?: false
                if (!success) {
                    continuation.resume(
                        StripePaymentResult.Failed(
                            message = userInfo.stringValue("errorMessage")
                                ?: "Stripe PaymentSheet iOS bridge failed."
                        )
                    )
                    return@addObserverForName
                }

                val status = userInfo.stringValue("status")
                    ?.lowercase()
                    ?: "failed"

                when (status) {
                    "completed" -> {
                        val paymentIntentId = userInfo.stringValue("paymentIntentId")
                            ?: paymentIntentIdFromClientSecret(clientSecret)
                        continuation.resume(
                            StripePaymentResult.Completed(paymentIntentId = paymentIntentId)
                        )
                    }

                    "canceled" -> continuation.resume(StripePaymentResult.Canceled)
                    else -> continuation.resume(
                        StripePaymentResult.Failed(
                            message = userInfo.stringValue("errorMessage") ?: "Stripe payment failed."
                        )
                    )
                }
            }

            NSNotificationCenter.defaultCenter.postNotificationName(
                REQUEST_NOTIFICATION,
                null,
                mapOf<Any?, Any?>(
                    "requestId" to requestId,
                    "action" to "presentPaymentSheet",
                    "clientSecret" to clientSecret,
                    "customerName" to customerInfo.name,
                    "customerEmail" to customerInfo.email,
                    "customerPhone" to customerInfo.phone,
                ),
            )

            continuation.invokeOnCancellation {
                observer?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
            }
        }
    }
}

private fun NSDictionary.stringValue(key: String): String? {
    val value = objectForKey(key) ?: return null
    return when (value) {
        is String -> value
        is NSString -> value.toString()
        else -> value.toString()
    }?.takeIf { it.isNotBlank() }
}

private fun NSDictionary.longValue(key: String): Long? {
    val raw = objectForKey(key) ?: return null
    return when (raw) {
        is Long -> raw
        is Int -> raw.toLong()
        is Number -> raw.toLong()
        is NSString -> raw.toString().toLongOrNull()
        is String -> raw.toLongOrNull()
        else -> null
    }
}

private fun NSDictionary.boolValue(key: String): Boolean? {
    val raw = objectForKey(key) ?: return null
    return when (raw) {
        is Boolean -> raw
        is Number -> raw.toInt() != 0
        is NSString -> parseBoolean(raw.toString())
        is String -> parseBoolean(raw)
        else -> null
    }
}

private fun parseBoolean(value: String): Boolean? {
    return when (value.trim().lowercase()) {
        "true", "1", "yes" -> true
        "false", "0", "no" -> false
        else -> null
    }
}
