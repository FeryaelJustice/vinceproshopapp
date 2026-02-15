package com.billiardsdraw.vinceproshop.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billiardsdraw.vinceproshop.core.DispatchersProvider
import com.billiardsdraw.vinceproshop.domain.model.FeaturedSlide
import com.billiardsdraw.vinceproshop.domain.model.Product
import com.billiardsdraw.vinceproshop.domain.usecase.ObserveHomeFeedUseCase
import com.billiardsdraw.vinceproshop.domain.usecase.RefreshCatalogUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val featuredSlides: List<FeaturedSlide> = emptyList(),
    val quickViewProducts: List<Product> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

class HomeViewModel(
    private val refreshCatalog: RefreshCatalogUseCase,
    observeHomeFeed: ObserveHomeFeedUseCase,
    private val dispatchers: DispatchersProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch(dispatchers.io) {
            runCatching { refreshCatalog() }
                .onFailure { error ->
                    _state.value =
                        _state.value.copy(
                            isLoading = false,
                            errorMessage = error.message,
                        )
                }
        }

        viewModelScope.launch(dispatchers.main) {
            observeHomeFeed().collect { feed ->
                _state.value =
                    _state.value.copy(
                        featuredSlides = feed.featured,
                        quickViewProducts = feed.quickView,
                        isLoading = false,
                        errorMessage = null,
                    )
            }
        }
    }

    fun retry() {
        _state.value = _state.value.copy(isLoading = true)
        viewModelScope.launch(dispatchers.io) {
            runCatching { refreshCatalog() }
                .onFailure { error ->
                    _state.value =
                        _state.value.copy(
                            isLoading = false,
                            errorMessage = error.message,
                        )
                }
        }
    }
}
