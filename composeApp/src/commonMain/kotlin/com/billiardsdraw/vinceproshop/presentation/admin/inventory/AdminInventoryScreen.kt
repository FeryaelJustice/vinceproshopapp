package com.billiardsdraw.vinceproshop.presentation.admin.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.billiardsdraw.vinceproshop.core.formatEuro
import com.billiardsdraw.vinceproshop.data.remote.CategoryDto
import com.billiardsdraw.vinceproshop.presentation.common.tr
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AdminInventoryScreen(
    modifier: Modifier = Modifier,
    viewModel: AdminInventoryViewModel = koinViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value

    Column(modifier = modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = tr("Inventory", "Inventario"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            TextButton(onClick = viewModel::refresh) {
                Text(tr("Refresh", "Actualizar"))
            }
        }

        when {
            state.isLoading -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            }

            state.error != null -> Text(state.error, color = MaterialTheme.colorScheme.error)
            state.products.isEmpty() -> Text(tr("No products found.", "No hay productos."))
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.products, key = { it.id }) { product ->
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "${product.name} (${product.slug})",
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text("${tr("Category", "Categoria")}: ${categoryBreadcrumb(product.categoryId, state.categories)}")
                            Text(
                                "${tr("Status", "Estado")}: " +
                                    if (product.discontinued == 1) tr("Discontinued", "Descontinuado") else tr("Active", "Activo")
                            )
                            Text("${tr("Out Of Stock Interested", "Interesados sin stock")}: ${product.stockInterestCount}")

                            if (product.sizes.isEmpty()) {
                                Text(tr("No sizes configured", "Sin tallas configuradas"))
                            } else {
                                product.sizes.forEach { size ->
                                    Text(
                                        "- ${size.sizeName}: qty ${size.quantity}, ${formatEuro(size.price)} (disc ${size.discount})",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun categoryBreadcrumb(categoryId: String, categories: List<CategoryDto>): String {
    if (categoryId.isBlank()) return "-"
    val byId = categories.associateBy { it.id }
    val visited = mutableSetOf<String>()
    val labels = mutableListOf<String>()
    var current: String? = categoryId
    while (!current.isNullOrBlank() && visited.add(current)) {
        val category = byId[current] ?: break
        labels.add(category.name)
        current = category.parentId
    }
    if (labels.isEmpty()) return categoryId
    return labels.reversed().joinToString(" / ")
}
