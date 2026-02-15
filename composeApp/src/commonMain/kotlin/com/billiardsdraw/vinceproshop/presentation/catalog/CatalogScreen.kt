package com.billiardsdraw.vinceproshop.presentation.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.billiardsdraw.vinceproshop.core.isSpanishLanguage
import com.billiardsdraw.vinceproshop.domain.model.Category
import com.billiardsdraw.vinceproshop.domain.model.localizedName
import com.billiardsdraw.vinceproshop.presentation.common.tr
import com.billiardsdraw.vinceproshop.presentation.components.EmptyState
import com.billiardsdraw.vinceproshop.presentation.components.ProductCard

data class CatalogRenderItem(
    val slug: String,
    val cue: Boolean,
)

@Composable
fun CatalogScreen(
    state: CatalogUiState,
    languageCode: String,
    onOpenProduct: (String) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onToggleBrand: (String) -> Unit,
    onMinPriceChanged: (String) -> Unit,
    onMaxPriceChanged: (String) -> Unit,
    onAvailabilityChanged: (AvailabilityFilter) -> Unit,
    onSortChanged: (CatalogSort) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val renderItems = remember(state.cueProducts, state.regularProducts) {
        buildList {
            state.cueProducts.forEach { add(CatalogRenderItem(it.slug, true)) }
            state.regularProducts.forEach { add(CatalogRenderItem(it.slug, false)) }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 180.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            FilterPanel(
                state = state,
                languageCode = languageCode,
                onSelectCategory = onSelectCategory,
                onToggleBrand = onToggleBrand,
                onMinPriceChanged = onMinPriceChanged,
                onMaxPriceChanged = onMaxPriceChanged,
                onAvailabilityChanged = onAvailabilityChanged,
                onSortChanged = onSortChanged,
                onClearFilters = onClearFilters,
            )
        }

        if (state.isLoading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = tr("Loading catalog...", "Cargando catalogo..."),
                    modifier = Modifier.padding(8.dp),
                )
            }
        }

        if (!state.isLoading && renderItems.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(
                    title = tr("No results", "Sin resultados"),
                    description = tr(
                        "Adjust filters or search for another product.",
                        "Ajusta filtros o busca otro producto.",
                    ),
                )
            }
        }

        items(
            items = renderItems,
            key = { it.slug },
            span = { item -> if (item.cue) GridItemSpan(maxLineSpan) else GridItemSpan(1) },
        ) { entry ->
            val product = (state.cueProducts + state.regularProducts).firstOrNull { it.slug == entry.slug } ?: return@items
            ProductCard(
                product = product,
                languageCode = languageCode,
                isCueLayout = entry.cue,
                onClick = { onOpenProduct(product.slug) },
            )
        }

        state.errorMessage?.let { message ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun FilterPanel(
    state: CatalogUiState,
    languageCode: String,
    onSelectCategory: (String?) -> Unit,
    onToggleBrand: (String) -> Unit,
    onMinPriceChanged: (String) -> Unit,
    onMaxPriceChanged: (String) -> Unit,
    onAvailabilityChanged: (AvailabilityFilter) -> Unit,
    onSortChanged: (CatalogSort) -> Unit,
    onClearFilters: () -> Unit,
) {
    val brands = remember(state.products) { state.products.map { it.vendor }.distinct().sorted() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = tr("Filters", "Filtros"),
            style = MaterialTheme.typography.titleLarge,
        )

        ChipGroup(
            title = tr("Availability", "Disponibilidad"),
            options = listOf(
                tr("All", "Todos") to (state.availability == AvailabilityFilter.All),
                tr("In stock", "En stock") to (state.availability == AvailabilityFilter.InStock),
                tr("Out of stock", "Sin stock") to (state.availability == AvailabilityFilter.OutOfStock),
            ),
            onClick = { index ->
                onAvailabilityChanged(
                    when (index) {
                        1 -> AvailabilityFilter.InStock
                        2 -> AvailabilityFilter.OutOfStock
                        else -> AvailabilityFilter.All
                    }
                )
            },
        )

        ChipGroup(
            title = tr("Sort", "Ordenar"),
            options = listOf(
                tr("A-Z", "A-Z") to (state.sort == CatalogSort.AlphaAsc),
                tr("Z-A", "Z-A") to (state.sort == CatalogSort.AlphaDesc),
                tr("Price low", "Precio menor") to (state.sort == CatalogSort.PriceAsc),
                tr("Price high", "Precio mayor") to (state.sort == CatalogSort.PriceDesc),
            ),
            onClick = { index ->
                onSortChanged(
                    when (index) {
                        1 -> CatalogSort.AlphaDesc
                        2 -> CatalogSort.PriceAsc
                        3 -> CatalogSort.PriceDesc
                        else -> CatalogSort.AlphaAsc
                    }
                )
            },
        )

        ChipGroup(
            title = tr("Category", "Categoria"),
            options = categoryOptions(state.categories, languageCode, state.selectedCategoryId),
            onClick = { index ->
                if (index == 0) {
                    onSelectCategory(null)
                } else {
                    onSelectCategory(state.categories[index - 1].id)
                }
            },
        )

        if (brands.isNotEmpty()) {
            ChipGroup(
                title = tr("Brands", "Marcas"),
                options = brands.map { brand ->
                    brand to state.selectedBrands.contains(brand)
                },
                onClick = { index -> onToggleBrand(brands[index]) },
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.minPrice,
                onValueChange = onMinPriceChanged,
                singleLine = true,
                label = { Text(tr("Min price", "Precio min")) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.maxPrice,
                onValueChange = onMaxPriceChanged,
                singleLine = true,
                label = { Text(tr("Max price", "Precio max")) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Button(onClick = onClearFilters) {
            Text(tr("Reset filters", "Limpiar filtros"))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipGroup(
    title: String,
    options: List<Pair<String, Boolean>>,
    onClick: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEachIndexed { index, (label, selected) ->
                AssistChip(
                    onClick = { onClick(index) },
                    label = { Text(label) },
                    leadingIcon = if (selected) {
                        { Text("●", color = MaterialTheme.colorScheme.primary) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

private fun categoryOptions(
    categories: List<Category>,
    languageCode: String,
    selectedCategoryId: String?,
): List<Pair<String, Boolean>> {
    val allLabel = if (isSpanishLanguage()) "Todas" else "All"
    val base = listOf(allLabel to (selectedCategoryId == null))
    return base + categories.map { category ->
        category.localizedName(languageCode) to (selectedCategoryId == category.id)
    }
}
