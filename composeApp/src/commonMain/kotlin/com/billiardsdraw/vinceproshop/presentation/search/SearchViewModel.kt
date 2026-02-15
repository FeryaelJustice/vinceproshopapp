package com.billiardsdraw.vinceproshop.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billiardsdraw.vinceproshop.core.DispatchersProvider
import com.billiardsdraw.vinceproshop.domain.model.Product
import com.billiardsdraw.vinceproshop.domain.usecase.ObserveCatalogUseCase
import com.billiardsdraw.vinceproshop.domain.usecase.SearchProductsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val allProducts: List<Product> = emptyList(),
    val results: List<Product> = emptyList(),
    val isLoading: Boolean = true,
)

class SearchViewModel(
    observeCatalog: ObserveCatalogUseCase,
    private val searchProducts: SearchProductsUseCase,
    dispatchers: DispatchersProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch(dispatchers.main) {
            observeCatalog().collect { feed ->
                val query = _state.value.query
                val results = searchProducts(feed.products, query)
                _state.value = _state.value.copy(
                    allProducts = feed.products,
                    results = results,
                    isLoading = false,
                )
            }
        }
    }

    fun onQueryChanged(value: String) {
        val results = searchProducts(_state.value.allProducts, value)
        _state.value = _state.value.copy(
            query = value,
            results = results,
        )
    }
}
