package com.billiardsdraw.vinceproshop.presentation.product

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.billiardsdraw.vinceproshop.core.formatEuro
import com.billiardsdraw.vinceproshop.domain.model.localizedDescription
import com.billiardsdraw.vinceproshop.domain.model.localizedName
import com.billiardsdraw.vinceproshop.presentation.common.tr

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductDetailScreen(
    state: ProductDetailUiState,
    languageCode: String,
    onBack: () -> Unit,
    onSelectSize: (String) -> Unit,
    onQuantityChange: (Int) -> Unit,
    onImageSelect: (Int) -> Unit,
    onAddToCart: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val product = state.product
    if (state.isLoading || product == null) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(tr("Loading product...", "Cargando producto..."))
        }
        return
    }

    val images = buildList {
        if (product.imageUrl.isNotBlank()) add(product.imageUrl)
        addAll(product.media.map { it.url }.filter { it.isNotBlank() })
    }.distinct()
    val selectedImage = images.getOrNull(state.selectedImageIndex) ?: product.imageUrl

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = tr("Back to catalog", "Volver al catalogo"),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onBack),
            )
        }

        item {
            AsyncImage(
                model = selectedImage,
                contentDescription = product.localizedName(languageCode),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop,
            )
        }

        if (images.size > 1) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(images) { index, imageUrl ->
                        Surface(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onImageSelect(index) },
                            tonalElevation = if (index == state.selectedImageIndex) 4.dp else 0.dp,
                        ) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = product.localizedName(languageCode),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = product.vendor,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatEuro(product.price),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = product.localizedDescription(languageCode),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        if (product.options.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(tr("Size", "Talla"), style = MaterialTheme.typography.titleMedium)
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        product.options.forEach { option ->
                            AssistChip(
                                onClick = { onSelectSize(option.value) },
                                label = { Text(option.label.ifBlank { option.value }) },
                                leadingIcon = if (option.value == state.selectedSize) {
                                    { Text("●", color = MaterialTheme.colorScheme.primary) }
                                } else null,
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(tr("Quantity", "Cantidad"))
                IconButton(onClick = { onQuantityChange(state.quantity - 1) }) {
                    Text("-")
                }
                Text(state.quantity.toString())
                IconButton(onClick = { onQuantityChange(state.quantity + 1) }) {
                    Text("+")
                }
            }
        }

        item {
            Button(onClick = onAddToCart, modifier = Modifier.fillMaxWidth()) {
                Text(tr("Add to cart", "Agregar al carrito"))
            }
        }

        state.message?.let {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = tr("Added to cart", "Agregado al carrito"),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = tr("Dismiss", "Cerrar"),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = onDismissMessage),
                    )
                }
            }
        }
    }
}
