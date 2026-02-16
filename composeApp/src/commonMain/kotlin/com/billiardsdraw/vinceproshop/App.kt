package com.billiardsdraw.vinceproshop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.billiardsdraw.vinceproshop.core.LocalizationManager
import com.billiardsdraw.vinceproshop.core.isRtlLanguageCode
import com.billiardsdraw.vinceproshop.core.rememberCurrentLanguageCodeState
import com.billiardsdraw.vinceproshop.core.rememberLanguageOptionState
import com.billiardsdraw.vinceproshop.di.appModule
import com.billiardsdraw.vinceproshop.presentation.admin.AdminPanelScreen
import com.billiardsdraw.vinceproshop.presentation.account.AccountSheet
import com.billiardsdraw.vinceproshop.presentation.account.AccountViewModel
import com.billiardsdraw.vinceproshop.presentation.cart.CartScreen
import com.billiardsdraw.vinceproshop.presentation.cart.CartViewModel
import com.billiardsdraw.vinceproshop.presentation.catalog.CatalogScreen
import com.billiardsdraw.vinceproshop.presentation.catalog.CatalogViewModel
import com.billiardsdraw.vinceproshop.presentation.chatbot.ChatbotFloatingWidget
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

@Suppress("ModifierRequired","ktlint:standard:function-naming")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    KoinApplication(
        application = {
            modules(appModule)
        }
    ) {
        VinceTheme {
            val navigator = remember { AppNavigator() }
            val languageCode by rememberCurrentLanguageCodeState()
            val languageOption by rememberLanguageOptionState()

            val homeViewModel = koinViewModel<HomeViewModel>(key = "home")
            val catalogViewModel = koinViewModel<CatalogViewModel>(key = "catalog")
            val searchViewModel = koinViewModel<SearchViewModel>(key = "search")
            val cartViewModel = koinViewModel<CartViewModel>(key = "cart")
            val accountViewModel = koinViewModel<AccountViewModel>(key = "account")
            var isAccountSheetVisible by remember { mutableStateOf(false) }

            val homeState by homeViewModel.state.collectAsStateWithLifecycle()
            val catalogState by catalogViewModel.state.collectAsStateWithLifecycle()
            val searchState by searchViewModel.state.collectAsStateWithLifecycle()
            val cartState by cartViewModel.state.collectAsStateWithLifecycle()
            val accountState by accountViewModel.state.collectAsStateWithLifecycle()
            val accountSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

            if (isAccountSheetVisible) {
                ModalBottomSheet(
                    onDismissRequest = { isAccountSheetVisible = false },
                    sheetState = accountSheetState,
                ) {
                    AccountSheet(
                        state = accountState,
                        languageCode = languageCode,
                        languageOption = languageOption,
                        onLanguageOptionChange = LocalizationManager::setLanguageOption,
                        onLogin = accountViewModel::login,
                        onLogout = accountViewModel::logout,
                        onRefresh = accountViewModel::refresh,
                        onClearSavedCredential = accountViewModel::clearSavedCredential,
                        onDismissAuthError = accountViewModel::clearAuthError,
                        onOpenAdminRoute = { route ->
                            isAccountSheetVisible = false
                            navigator.switchAdmin(route)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            CompositionLocalProvider(
                androidx.compose.ui.platform.LocalLayoutDirection provides
                    if (isRtlLanguageCode(languageCode)) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                Scaffold(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars),
                    topBar = {
                        VinceTopBar(
                            cartCount = cartState.totalItems,
                            onCartClick = { navigator.openRoot(RootSection.Cart) },
                        )
                    },
                    bottomBar = {
                        if (navigator.current !is AppDestination.ProductDetail &&
                            navigator.current !is AppDestination.AdminRoute
                        ) {
                            VinceBottomBar(
                                selectedRoot = navigator.selectedSection(),
                                isAccountSelected = isAccountSheetVisible,
                                onSelectRoot = { section ->
                                    isAccountSheetVisible = false
                                    navigator.openRoot(section)
                                },
                                onAccountClick = {
                                    isAccountSheetVisible = true
                                    accountViewModel.refresh()
                                },
                            )
                        }
                    },
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { padding ->
                    Box(modifier = Modifier.fillMaxSize()) {
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
                                        onMinPriceChange = catalogViewModel::onMinPriceChanged,
                                        onMaxPriceChange = catalogViewModel::onMaxPriceChanged,
                                        onAvailabilityChange = catalogViewModel::onAvailabilityChanged,
                                        onSortChange = catalogViewModel::onSortChanged,
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
                                        onCustomerInfoChanged = cartViewModel::updateCustomerInfo,
                                        onContinueToPayment = cartViewModel::continueToPayment,
                                        onBackToShipping = cartViewModel::backToShipping,
                                        onPaymentStarted = cartViewModel::onPaymentStarted,
                                        onPaymentCompleted = cartViewModel::onPaymentCompleted,
                                        onPaymentCanceled = cartViewModel::onPaymentCanceled,
                                        onPaymentFailed = cartViewModel::onPaymentFailed,
                                        onResetCheckout = cartViewModel::resetCheckoutFlow,
                                        onDismissCheckoutFeedback = cartViewModel::dismissCheckoutFeedback,
                                        modifier = Modifier.padding(padding),
                                    )
                                }
                            }

                            is AppDestination.ProductDetail -> {
                                val detailViewModel: ProductDetailViewModel = koinViewModel(
                                    key = "detail-${destination.slug}",
                                    parameters = { parametersOf(destination.slug) },
                                )
                                val detailState by detailViewModel.state.collectAsStateWithLifecycle()
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

                            is AppDestination.AdminRoute -> {
                                AdminPanelScreen(
                                    currentRoute = destination.route,
                                    languageCode = languageCode,
                                    onSelectRoute = navigator::switchAdmin,
                                    onBack = navigator::back,
                                    modifier = Modifier.padding(padding),
                                )
                            }
                        }

                        if (navigator.current !is AppDestination.AdminRoute) {
                            ChatbotFloatingWidget(
                                languageCode = languageCode,
                                currentPath = navigator.current.toChatbotPath(),
                                contentPadding = padding,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun AppDestination.toChatbotPath(): String =
    when (this) {
        is AppDestination.Root ->
            when (section) {
                RootSection.Home -> "/"
                RootSection.Catalog -> "/products"
                RootSection.Search -> "/search"
                RootSection.Cart -> "/cart"
            }

        is AppDestination.ProductDetail -> "/products/$slug"
        is AppDestination.AdminRoute -> route.ifBlank { "/admin" }
    }
