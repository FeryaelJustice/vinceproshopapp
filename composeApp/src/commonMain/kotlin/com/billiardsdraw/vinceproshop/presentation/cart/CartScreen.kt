package com.billiardsdraw.vinceproshop.presentation.cart

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.billiardsdraw.vinceproshop.core.formatEuro
import com.billiardsdraw.vinceproshop.domain.model.CartItem
import com.billiardsdraw.vinceproshop.domain.model.finalPrice
import com.billiardsdraw.vinceproshop.presentation.common.tr
import com.billiardsdraw.vinceproshop.presentation.components.EmptyState

@Composable
fun CartScreen(
    state: CartUiState,
    languageCode: String,
    onUpdateQuantity: (CartItem, Int) -> Unit,
    onRemove: (CartItem) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.isLoading && state.items.isEmpty()) {
        EmptyState(
            title = tr("Your cart is empty", "Tu carrito esta vacio"),
            description = tr(
                "Add products from the catalog.",
                "Agrega productos desde el catalogo.",
            ),
            modifier = modifier,
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(
            items = state.items,
            key = { "${it.slug}-${it.size}" },
            contentType = { _ -> "contentType1" }) { item ->
            CartItemCard(
                item = item,
                languageCode = languageCode,
                onUpdateQuantity = onUpdateQuantity,
                onRemove = onRemove,
            )
        }

        item(contentType = "contentType2") {
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
                Button(onClick = onClearAll, modifier = Modifier.fillMaxWidth()) {
                    Text(tr("Clear cart", "Vaciar carrito"))
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    verticalArrangement = Arrangement.spacedBy(4.dp)
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
                    verticalAlignment = Alignment.CenterVertically
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
