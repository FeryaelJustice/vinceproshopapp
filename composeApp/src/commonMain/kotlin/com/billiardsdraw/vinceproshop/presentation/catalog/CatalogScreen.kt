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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.billiardsdraw.vinceproshop.core.isSpanishLanguage
import com.billiardsdraw.vinceproshop.domain.model.Category
import com.billiardsdraw.vinceproshop.domain.model.localizedName
import com.billiardsdraw.vinceproshop.presentation.common.tr
import com.billiardsdraw.vinceproshop.presentation.components.EmptyState
import com.billiardsdraw.vinceproshop.presentation.components.ProductCard


private enum class CatalogItemType {
    FilterPanel,  // item de span completo con todos los filtros
    Loading,      // Text "Cargando catalogo..."
    Empty,        // EmptyState sin resultados
    Product,      // ProductCard (cue y regular — mismo composable, mismo tipo)
    Error,        // Text de errorMessage
}

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
    onMinPriceChange: (String) -> Unit,
    onMaxPriceChange: (String) -> Unit,
    onAvailabilityChange: (AvailabilityFilter) -> Unit,
    onSortChange: (CatalogSort) -> Unit,
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
        item(span = { GridItemSpan(maxLineSpan) }, contentType = CatalogItemType.FilterPanel) {
            FilterPanel(
                state = state,
                languageCode = languageCode,
                onSelectCategory = onSelectCategory,
                onToggleBrand = onToggleBrand,
                onMinPriceChange = onMinPriceChange,
                onMaxPriceChange = onMaxPriceChange,
                onAvailabilityChange = onAvailabilityChange,
                onSortChange = onSortChange,
                onClearFilters = onClearFilters,
            )
        }

        if (state.isLoading) {
            item(span = { GridItemSpan(maxLineSpan) }, contentType = CatalogItemType.Loading) {
                Text(
                    text = tr("Loading catalog...", "Cargando catalogo..."),
                    modifier = Modifier.padding(8.dp),
                )
            }
        }

        if (!state.isLoading && renderItems.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, contentType = CatalogItemType.Empty) {
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
            contentType = { CatalogItemType.Product }
        ) { entry ->
            val product =
                (state.cueProducts + state.regularProducts).firstOrNull { it.slug == entry.slug }
                    ?: return@items
            ProductCard(
                product = product,
                languageCode = languageCode,
                isCueLayout = entry.cue,
                onClick = { onOpenProduct(product.slug) },
            )
        }

        state.errorMessage?.let { message ->
            item(span = { GridItemSpan(maxLineSpan) }, contentType = CatalogItemType.Error) {
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
    onMinPriceChange: (String) -> Unit,
    onMaxPriceChange: (String) -> Unit,
    onAvailabilityChange: (AvailabilityFilter) -> Unit,
    onSortChange: (CatalogSort) -> Unit,
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
                tr(
                    "Out of stock",
                    "Sin stock"
                ) to (state.availability == AvailabilityFilter.OutOfStock),
            ),
            onClick = { index ->
                onAvailabilityChange(
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
                onSortChange(
                    when (index) {
                        1 -> CatalogSort.AlphaDesc
                        2 -> CatalogSort.PriceAsc
                        3 -> CatalogSort.PriceDesc
                        else -> CatalogSort.AlphaAsc
                    }
                )
            },
        )

        CategoryDropdown(
            categories = state.categories,
            languageCode = languageCode,
            selectedCategoryId = state.selectedCategoryId,
            onSelectCategory = onSelectCategory,
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
                onValueChange = onMinPriceChange,
                singleLine = true,
                label = { Text(tr("Min price", "Precio min")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = tr("Minimum price input", "Campo de precio minimo")
                    },
            )
            OutlinedTextField(
                value = state.maxPrice,
                onValueChange = onMaxPriceChange,
                singleLine = true,
                label = { Text(tr("Max price", "Precio max")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = tr("Maximum price input", "Campo de precio maximo")
                    },
            )
        }

        Button(onClick = onClearFilters) {
            Text(tr("Reset filters", "Limpiar filtros"))
        }
    }
}

data class CategoryTreeOption(
    val id: String,
    val label: String,
    val depth: Int,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<Category>,
    languageCode: String,
    selectedCategoryId: String?,
    onSelectCategory: (String?) -> Unit,
) {
    val allLabel = if (isSpanishLanguage()) "Todas" else "All"
    val options = remember(categories, languageCode) {
        buildCategoryTreeOptions(categories, languageCode)
    }
    val selectedLabel = remember(categories, languageCode, selectedCategoryId) {
        if (selectedCategoryId == null) {
            allLabel
        } else {
            categories.firstOrNull { it.id == selectedCategoryId }?.localizedName(languageCode)
                ?: allLabel
        }
    }
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = tr("Category", "Categoria"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
        ) {
            OutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = tr("Category selector", "Selector de categoria")
                    },
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(allLabel) },
                    onClick = {
                        onSelectCategory(null)
                        expanded = false
                    },
                )

                if (options.isNotEmpty()) {
                    HorizontalDivider()
                }

                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            val indent = "   ".repeat(option.depth)
                            val prefix = if (option.depth > 0) "- " else ""
                            Text("$indent$prefix${option.label}")
                        },
                        onClick = {
                            onSelectCategory(option.id)
                            expanded = false
                        },
                    )
                }
            }
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

private fun buildCategoryTreeOptions(
    categories: List<Category>,
    languageCode: String,
): List<CategoryTreeOption> {
    if (categories.isEmpty()) return emptyList()

    val byId = categories.associateBy { it.id }
    val childrenByParent = categories.groupBy { category ->
        category.parentId?.takeIf { byId.containsKey(it) }
    }
    val visited = mutableSetOf<String>()
    val result = mutableListOf<CategoryTreeOption>()

    fun visit(parentId: String?, depth: Int) {
        childrenByParent[parentId]
            .orEmpty()
            .sortedBy { it.localizedName(languageCode).lowercase() }
            .forEach { child ->
                if (!visited.add(child.id)) return@forEach
                result += CategoryTreeOption(
                    id = child.id,
                    label = child.localizedName(languageCode),
                    depth = depth,
                )
                visit(child.id, depth + 1)
            }
    }

    visit(parentId = null, depth = 0)

    categories
        .sortedBy { it.localizedName(languageCode).lowercase() }
        .forEach { category ->
            if (!visited.contains(category.id)) {
                result += CategoryTreeOption(
                    id = category.id,
                    label = category.localizedName(languageCode),
                    depth = 0,
                )
                visit(category.id, 1)
            }
        }

    return result
}
