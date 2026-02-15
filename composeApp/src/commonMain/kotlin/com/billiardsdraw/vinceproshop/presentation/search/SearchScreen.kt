package com.billiardsdraw.vinceproshop.presentation.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.billiardsdraw.vinceproshop.presentation.common.tr
import com.billiardsdraw.vinceproshop.presentation.components.EmptyState
import com.billiardsdraw.vinceproshop.presentation.components.ProductCard

private enum class SearchItemType {
    SearchField, // OutlinedTextField de búsqueda
    SectionTitle, // Text "Resultados"
    Empty, // EmptyState (query vacío Y sin resultados — mismo composable)
    Product, // ProductCard
}

@Composable
fun SearchScreen(
    state: SearchUiState,
    languageCode: String,
    onQueryChange: (String) -> Unit,
    onOpenProduct: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(contentType = SearchItemType.SearchField) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                singleLine = true,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription =
                                tr("Search products input", "Campo de busqueda de productos")
                        },
                label = { Text(tr("Search products", "Buscar productos")) },
            )
        }

        item(contentType = SearchItemType.SectionTitle) {
            Text(
                text = tr("Results", "Resultados"),
                style = MaterialTheme.typography.titleLarge,
            )
        }

        if (state.query.isBlank()) {
            item(contentType = SearchItemType.Empty) {
                EmptyState(
                    title = tr("Start typing", "Empieza a escribir"),
                    description =
                        tr(
                            "Search by product name or brand.",
                            "Busca por nombre de producto o marca.",
                        ),
                )
            }
        } else if (state.results.isEmpty()) {
            item(contentType = SearchItemType.Empty) {
                EmptyState(
                    title = tr("No matches", "Sin coincidencias"),
                    description =
                        tr(
                            "Try another keyword.",
                            "Prueba otra palabra clave.",
                        ),
                )
            }
        } else {
            items(
                state.results,
                key = { it.slug },
                contentType = { SearchItemType.Product },
            ) { product ->
                ProductCard(
                    product = product,
                    languageCode = languageCode,
                    isCueLayout = false,
                    onClick = { onOpenProduct(product.slug) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
