package com.billiardsdraw.vinceproshop

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.billiardsdraw.vinceproshop.core.currentLanguageCode
import com.billiardsdraw.vinceproshop.di.appModule
import com.billiardsdraw.vinceproshop.presentation.cart.CartScreen
import com.billiardsdraw.vinceproshop.presentation.cart.CartViewModel
import com.billiardsdraw.vinceproshop.presentation.catalog.CatalogScreen
import com.billiardsdraw.vinceproshop.presentation.catalog.CatalogViewModel
import com.billiardsdraw.vinceproshop.presentation.components.VinceBottomBar
import com.billiardsdraw.vinceproshop.presentation.components.VinceTopBar
import com.billiardsdraw.vinceproshop.presentation.home.HomeScreen
import com.billiardsdraw.vinceproshop.presentation.home.HomeViewModel
import com.billiardsdraw.vinceproshop.presentation.navigation.AppDestination
import com.billiardsdraw.vinceproshop.presentation.navigation.AppNavigator
import com.billiardsdraw.vinceproshop.presentation.navigation.RootSection
import com.billiardsdraw.vinceproshop.presentation.product.ProductDetailScreen
import com.billiardsdraw.vinceproshop.presentation.product.ProductDetailViewModel
import com.billiardsdraw.vinceproshop.presentation.search.SearchScreen
import com.billiardsdraw.vinceproshop.presentation.search.SearchViewModel
import com.billiardsdraw.vinceproshop.presentation.theme.VinceTheme
import org.koin.compose.KoinApplication
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun App() {
    KoinApplication(
        application = {
            modules(appModule)
        }
    ) {
        VinceTheme {
            val navigator = remember { AppNavigator() }
            val languageCode = currentLanguageCode()

            val homeViewModel = koinViewModel<HomeViewModel>(key = "home")
            val catalogViewModel = koinViewModel<CatalogViewModel>(key = "catalog")
            val searchViewModel = koinViewModel<SearchViewModel>(key = "search")
            val cartViewModel = koinViewModel<CartViewModel>(key = "cart")

            val homeState by homeViewModel.state.collectAsStateWithLifecycle()
            val catalogState by catalogViewModel.state.collectAsStateWithLifecycle()
            val searchState by searchViewModel.state.collectAsStateWithLifecycle()
            val cartState by cartViewModel.state.collectAsStateWithLifecycle()

            Scaffold(
                topBar = {
                    VinceTopBar(
                        cartCount = cartState.totalItems,
                        onCartClick = { navigator.openRoot(RootSection.Cart) },
                    )
                },
                bottomBar = {
                    if (navigator.current !is AppDestination.ProductDetail) {
                        VinceBottomBar(
                            selected = navigator.selectedSection(),
                            onSelect = { section -> navigator.openRoot(section) },
                        )
                    }
                },
                contentWindowInsets = WindowInsets.systemBars
            ) { padding ->
                when (val destination = navigator.current) {
                    is AppDestination.Root -> {
                        when (destination.section) {
                            RootSection.Home -> HomeScreen(
                                state = homeState,
                                languageCode = languageCode,
                                onOpenProduct = { slug -> navigator.openProduct(slug) },
                                onOpenCategory = { categoryId ->
                                    catalogViewModel.onCategorySelected(categoryId)
                                    navigator.openRoot(RootSection.Catalog)
                                },
                                onRetry = homeViewModel::retry,
                                modifier = Modifier.padding(padding),
                            )

                            RootSection.Catalog -> CatalogScreen(
                                state = catalogState,
                                languageCode = languageCode,
                                onOpenProduct = { slug -> navigator.openProduct(slug) },
                                onSelectCategory = catalogViewModel::onCategorySelected,
                                onToggleBrand = catalogViewModel::onBrandToggled,
                                onMinPriceChanged = catalogViewModel::onMinPriceChanged,
                                onMaxPriceChanged = catalogViewModel::onMaxPriceChanged,
                                onAvailabilityChanged = catalogViewModel::onAvailabilityChanged,
                                onSortChanged = catalogViewModel::onSortChanged,
                                onClearFilters = catalogViewModel::clearFilters,
                                modifier = Modifier.padding(padding),
                            )

                            RootSection.Search -> SearchScreen(
                                state = searchState,
                                languageCode = languageCode,
                                onQueryChange = searchViewModel::onQueryChanged,
                                onOpenProduct = { slug -> navigator.openProduct(slug) },
                                modifier = Modifier.padding(padding),
                            )

                            RootSection.Cart -> CartScreen(
                                state = cartState,
                                languageCode = languageCode,
                                onUpdateQuantity = cartViewModel::updateQuantity,
                                onRemove = cartViewModel::remove,
                                onClearAll = cartViewModel::clearAll,
                                modifier = Modifier.padding(padding),
                            )
                        }
                    }

                    is AppDestination.ProductDetail -> {
                        val detailViewModel: ProductDetailViewModel = koinViewModel(
                            key = "detail-${destination.slug}",
                            parameters = { parametersOf(destination.slug) },
                        )
                        val detailState by detailViewModel.state.collectAsState()
                        ProductDetailScreen(
                            state = detailState,
                            languageCode = languageCode,
                            onBack = navigator::back,
                            onSelectSize = detailViewModel::onSizeSelected,
                            onQuantityChange = detailViewModel::onQuantityChanged,
                            onImageSelect = detailViewModel::onImageSelected,
                            onAddToCart = detailViewModel::addCurrentSelectionToCart,
                            onDismissMessage = detailViewModel::dismissMessage,
                            modifier = Modifier.padding(padding),
                        )
                    }
                }
            }
        }
    }
}
