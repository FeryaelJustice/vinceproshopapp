package com.billiardsdraw.vinceproshop.presentation.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billiardsdraw.vinceproshop.core.DispatchersProvider
import com.billiardsdraw.vinceproshop.domain.model.Category
import com.billiardsdraw.vinceproshop.domain.model.Product
import com.billiardsdraw.vinceproshop.domain.usecase.ObserveCatalogUseCase
import com.billiardsdraw.vinceproshop.domain.usecase.RefreshCatalogUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AvailabilityFilter {
    All,
    InStock,
    OutOfStock,
}

enum class CatalogSort {
    AlphaAsc,
    AlphaDesc,
    PriceAsc,
    PriceDesc,
}

data class CatalogUiState(
    val isLoading: Boolean = true,
    val products: List<Product> = emptyList(),
    val cueProducts: List<Product> = emptyList(),
    val regularProducts: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    val selectedBrands: Set<String> = emptySet(),
    val minPrice: String = "",
    val maxPrice: String = "",
    val availability: AvailabilityFilter = AvailabilityFilter.All,
    val sort: CatalogSort = CatalogSort.AlphaAsc,
    val errorMessage: String? = null,
)

class CatalogViewModel(
    private val observeCatalog: ObserveCatalogUseCase,
    private val refreshCatalog: RefreshCatalogUseCase,
    dispatchers: DispatchersProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(CatalogUiState())
    val state: StateFlow<CatalogUiState> = _state.asStateFlow()

    private var allProducts: List<Product> = emptyList()
    private var allCategories: List<Category> = emptyList()

    init {
        viewModelScope.launch(dispatchers.io) {
            runCatching { refreshCatalog() }
                .onFailure { error ->
                    _state.value =
                        _state.value.copy(
                            errorMessage = error.message,
                            isLoading = false,
                        )
                }
        }

        viewModelScope.launch(dispatchers.main) {
            observeCatalog().collect { feed ->
                allProducts = feed.products
                allCategories = feed.categories
                recalculateState()
            }
        }
    }

    fun onCategorySelected(categoryId: String?) {
        _state.value = _state.value.copy(selectedCategoryId = categoryId)
        recalculateState()
    }

    fun onBrandToggled(brand: String) {
        val selected = _state.value.selectedBrands.toMutableSet()
        if (selected.contains(brand)) {
            selected.remove(brand)
        } else {
            selected.add(brand)
        }
        _state.value = _state.value.copy(selectedBrands = selected)
        recalculateState()
    }

    fun onMinPriceChanged(value: String) {
        _state.value = _state.value.copy(minPrice = value)
        recalculateState()
    }

    fun onMaxPriceChanged(value: String) {
        _state.value = _state.value.copy(maxPrice = value)
        recalculateState()
    }

    fun onAvailabilityChanged(filter: AvailabilityFilter) {
        _state.value = _state.value.copy(availability = filter)
        recalculateState()
    }

    fun onSortChanged(sort: CatalogSort) {
        _state.value = _state.value.copy(sort = sort)
        recalculateState()
    }

    fun clearFilters() {
        _state.value =
            _state.value.copy(
                selectedCategoryId = null,
                selectedBrands = emptySet(),
                minPrice = "",
                maxPrice = "",
                availability = AvailabilityFilter.All,
                sort = CatalogSort.AlphaAsc,
            )
        recalculateState()
    }

    private fun recalculateState() {
        val snapshot = _state.value
        val categoryIds = buildCategoryScope(snapshot.selectedCategoryId, allCategories)
        val min = snapshot.minPrice.toDoubleOrNull()
        val max = snapshot.maxPrice.toDoubleOrNull()

        val filtered =
            allProducts
                .asSequence()
                .filter { product ->
                    categoryIds == null || categoryIds.contains(product.categoryId)
                }.filter { product ->
                    when (snapshot.availability) {
                        AvailabilityFilter.All -> true
                        AvailabilityFilter.InStock -> product.inStock > 0
                        AvailabilityFilter.OutOfStock -> product.inStock <= 0
                    }
                }.filter { product ->
                    snapshot.selectedBrands.isEmpty() || snapshot.selectedBrands.contains(product.vendor)
                }.filter { product ->
                    (min == null || product.price >= min) && (max == null || product.price <= max)
                }.toList()
                .sortedWith(sortComparator(snapshot.sort))

        val cueProducts = filtered.filter { isCueCategory(it.categoryId, allCategories) }
        val regularProducts = filtered.filterNot { isCueCategory(it.categoryId, allCategories) }

        _state.value =
            snapshot.copy(
                isLoading = false,
                products = filtered,
                cueProducts = cueProducts,
                regularProducts = regularProducts,
                categories = allCategories,
                errorMessage = null,
            )
    }

    private fun sortComparator(sort: CatalogSort): Comparator<Product> =
        when (sort) {
            CatalogSort.AlphaAsc -> compareBy { it.name.lowercase() }
            CatalogSort.AlphaDesc -> compareByDescending { it.name.lowercase() }
            CatalogSort.PriceAsc -> compareBy { it.price }
            CatalogSort.PriceDesc -> compareByDescending { it.price }
        }

    private fun buildCategoryScope(
        categoryId: String?,
        categories: List<Category>,
    ): Set<String>? {
        if (categoryId.isNullOrBlank()) return null
        val childrenByParent = categories.groupBy { it.parentId }
        val result = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.add(categoryId)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (!result.add(current)) continue
            childrenByParent[current].orEmpty().forEach { queue.add(it.id) }
        }
        return result
    }

    private fun isCueCategory(
        categoryId: String,
        categories: List<Category>,
    ): Boolean {
        val byId = categories.associateBy { it.id }
        var current: String? = categoryId
        val visited = mutableSetOf<String>()
        while (!current.isNullOrBlank() && visited.add(current)) {
            val category = byId[current] ?: return false
            if (category.isCue) return true
            current = category.parentId
        }
        return false
    }
}
