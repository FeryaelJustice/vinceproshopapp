package com.billiardsdraw.vinceproshop.presentation.cart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.billiardsdraw.vinceproshop.core.formatEuro
import com.billiardsdraw.vinceproshop.domain.model.CartItem
import com.billiardsdraw.vinceproshop.domain.model.CheckoutCustomerInfo
import com.billiardsdraw.vinceproshop.domain.model.finalPrice
import com.billiardsdraw.vinceproshop.presentation.common.tr
import com.billiardsdraw.vinceproshop.presentation.components.EmptyState
import com.billiardsdraw.vinceproshop.presentation.payment.StripePaymentButton
import com.billiardsdraw.vinceproshop.presentation.payment.StripePaymentResult

@Composable
fun CartScreen(
    state: CartUiState,
    languageCode: String,
    onUpdateQuantity: (CartItem, Int) -> Unit,
    onRemove: (CartItem) -> Unit,
    onClearAll: () -> Unit,
    onCustomerInfoChanged: (CheckoutCustomerInfo) -> Unit,
    onContinueToPayment: (String) -> Unit,
    onBackToShipping: () -> Unit,
    onPaymentStarted: () -> Unit,
    onPaymentCompleted: (String?) -> Unit,
    onPaymentCanceled: () -> Unit,
    onPaymentFailed: (String) -> Unit,
    onResetCheckout: () -> Unit,
    onDismissCheckoutFeedback: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.checkoutStep == CheckoutStep.Completed -> {
                CheckoutSuccess(
                    paymentIntentId = state.paymentIntentId,
                    onResetCheckout = onResetCheckout,
                    modifier = Modifier.fillMaxSize(),
                )
                return
            }

            !state.isLoading && state.items.isEmpty() -> {
                EmptyState(
                    title = tr("Your cart is empty", "Tu carrito esta vacio"),
                    description = tr(
                        "Add products from the catalog.",
                        "Agrega productos desde el catalogo.",
                    ),
                    modifier = Modifier.fillMaxSize(),
                )
                return
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(
                        items = state.items,
                        key = { "${it.slug}-${it.size}" },
                        contentType = { "cartItems" }
                    ) { item ->
                        CartItemCard(
                            item = item,
                            languageCode = languageCode,
                            onUpdateQuantity = onUpdateQuantity,
                            onRemove = onRemove,
                        )
                    }

                    item(contentType = { "checkoutPanel" }) {
                        CheckoutPanel(
                            state = state,
                            languageCode = languageCode,
                            onClearAll = onClearAll,
                            onCustomerInfoChanged = onCustomerInfoChanged,
                            onContinueToPayment = onContinueToPayment,
                            onBackToShipping = onBackToShipping,
                            onPaymentStarted = onPaymentStarted,
                            onPaymentCompleted = onPaymentCompleted,
                            onPaymentCanceled = onPaymentCanceled,
                            onPaymentFailed = onPaymentFailed,
                            onDismissCheckoutFeedback = onDismissCheckoutFeedback,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckoutPanel(
    state: CartUiState,
    languageCode: String,
    onClearAll: () -> Unit,
    onCustomerInfoChanged: (CheckoutCustomerInfo) -> Unit,
    onContinueToPayment: (String) -> Unit,
    onBackToShipping: () -> Unit,
    onPaymentStarted: () -> Unit,
    onPaymentCompleted: (String?) -> Unit,
    onPaymentCanceled: () -> Unit,
    onPaymentFailed: (String) -> Unit,
    onDismissCheckoutFeedback: () -> Unit,
) {
    val customer = state.customerInfo

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = tr(
                "Subtotal: ${formatEuro(state.subtotal)}",
                "Subtotal: ${formatEuro(state.subtotal)}"
            ),
            style = MaterialTheme.typography.titleLarge,
        )

        if (state.checkoutError != null) {
            Text(
                text = state.checkoutError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (state.checkoutMessage != null) {
            Text(
                text = state.checkoutMessage,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (state.checkoutError != null || state.checkoutMessage != null) {
            TextButton(onClick = onDismissCheckoutFeedback) {
                Text(tr("Dismiss", "Cerrar"))
            }
        }

        when (state.checkoutStep) {
            CheckoutStep.Shipping -> {
                Text(
                    text = tr("Shipping Information", "Informacion de envio"),
                    style = MaterialTheme.typography.titleMedium,
                )
                OutlinedTextField(
                    value = customer.name,
                    onValueChange = { onCustomerInfoChanged(customer.copy(name = it)) },
                    modifier = Modifier.fillMaxWidth().semantics {
                        contentDescription = tr("Name input", "Campo de nombre")
                    },
                    label = { Text(tr("Name", "Nombre")) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = customer.email,
                    onValueChange = { onCustomerInfoChanged(customer.copy(email = it)) },
                    modifier = Modifier.fillMaxWidth().semantics {
                        contentDescription = tr("Email input", "Campo de correo")
                    },
                    label = { Text(tr("Email", "Correo")) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = customer.address,
                    onValueChange = { onCustomerInfoChanged(customer.copy(address = it)) },
                    modifier = Modifier.fillMaxWidth().semantics {
                        contentDescription = tr("Address input", "Campo de direccion")
                    },
                    label = { Text(tr("Address", "Direccion")) },
                    singleLine = true,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = customer.city,
                        onValueChange = { onCustomerInfoChanged(customer.copy(city = it)) },
                        modifier = Modifier.weight(1f).semantics {
                            contentDescription = tr("City input", "Campo de ciudad")
                        },
                        label = { Text(tr("City", "Ciudad")) },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = customer.zipCode,
                        onValueChange = { onCustomerInfoChanged(customer.copy(zipCode = it)) },
                        modifier = Modifier.weight(1f).semantics {
                            contentDescription = tr("ZIP code input", "Campo de codigo postal")
                        },
                        label = { Text(tr("ZIP", "CP")) },
                        singleLine = true,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = customer.country,
                        onValueChange = { onCustomerInfoChanged(customer.copy(country = it)) },
                        modifier = Modifier.weight(1f).semantics {
                            contentDescription = tr("Country input", "Campo de pais")
                        },
                        label = { Text(tr("Country", "Pais")) },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = customer.phone,
                        onValueChange = { onCustomerInfoChanged(customer.copy(phone = it)) },
                        modifier = Modifier.weight(1f).semantics {
                            contentDescription = tr("Phone input", "Campo de telefono")
                        },
                        label = { Text(tr("Phone", "Telefono")) },
                        singleLine = true,
                    )
                }

                Button(
                    onClick = { onContinueToPayment(languageCode) },
                    enabled = !state.isCreatingPaymentIntent,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (state.isCreatingPaymentIntent) {
                            tr("Creating payment intent...", "Creando intento de pago...")
                        } else {
                            tr("Continue to payment", "Continuar al pago")
                        }
                    )
                }
            }

            CheckoutStep.Payment -> {
                Text(
                    text = tr("Payment", "Pago"),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = tr(
                        "Use the secure Stripe checkout sheet to confirm payment.",
                        "Usa la pasarela segura de Stripe para confirmar el pago.",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                val clientSecret = state.paymentClientSecret
                if (clientSecret.isNullOrBlank()) {
                    Text(
                        text = tr(
                            "Missing payment client secret. Go back and try again.",
                            "Falta el client secret de pago. Vuelve atras e intenta de nuevo.",
                        ),
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    StripePaymentButton(
                        clientSecret = clientSecret,
                        customerInfo = state.customerInfo,
                        amount = state.paymentAmount,
                        enabled = !state.isProcessingPayment,
                        onLaunch = onPaymentStarted,
                        onResult = { result ->
                            when (result) {
                                is StripePaymentResult.Completed -> onPaymentCompleted(result.paymentIntentId)
                                StripePaymentResult.Canceled -> onPaymentCanceled()
                                is StripePaymentResult.Failed -> onPaymentFailed(result.message)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                TextButton(
                    onClick = onBackToShipping,
                    enabled = !state.isProcessingPayment,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(tr("Back to shipping", "Volver a envio"))
                }
            }

            CheckoutStep.Completed -> Unit
        }

        Button(
            onClick = onClearAll,
            enabled = !state.isProcessingPayment && !state.isCreatingPaymentIntent,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(tr("Clear cart", "Vaciar carrito"))
        }
    }
}

@Composable
private fun CheckoutSuccess(
    paymentIntentId: String,
    onResetCheckout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = tr("Payment completed", "Pago completado"),
                    style = MaterialTheme.typography.titleLarge,
                )
                if (paymentIntentId.isNotBlank()) {
                    Text(
                        text = tr(
                            "Transaction ID: $paymentIntentId",
                            "ID de transaccion: $paymentIntentId",
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    text = tr(
                        "Order status is finalized by webhook on backend.",
                        "El estado final del pedido se confirma por webhook en backend.",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onResetCheckout) {
                    Text(tr("Continue shopping", "Seguir comprando"))
                }
            }
        }
    }
}

@Composable
private fun CartItemCard(
    item: CartItem,
    languageCode: String,
    onUpdateQuantity: (CartItem, Int) -> Unit,
    onRemove: (CartItem) -> Unit,
) {
    Surface(shape = RoundedCornerShape(14.dp), tonalElevation = 2.dp) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.name,
                    modifier = Modifier
                        .size(74.dp)
                        .height(74.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = if (languageCode.startsWith("es", true)) item.nameEs else item.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = tr("Size", "Talla") + ": ${item.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatEuro(item.finalPrice),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(tr("Qty", "Cant."))
                    TextButton(onClick = { onUpdateQuantity(item, item.quantity - 1) }) {
                        Text("-")
                    }
                    Text(
                        text = item.quantity.toString(),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                    TextButton(onClick = { onUpdateQuantity(item, item.quantity + 1) }) {
                        Text("+")
                    }
                }

                TextButton(onClick = { onRemove(item) }) {
                    Text(
                        text = tr("Remove", "Quitar"),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
