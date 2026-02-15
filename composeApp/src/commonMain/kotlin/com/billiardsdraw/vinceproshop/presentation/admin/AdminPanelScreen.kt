package com.billiardsdraw.vinceproshop.presentation.admin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.billiardsdraw.vinceproshop.presentation.admin.common.AdminShell
import com.billiardsdraw.vinceproshop.presentation.admin.inventory.AdminInventoryScreen
import com.billiardsdraw.vinceproshop.presentation.admin.manage.categories.AdminManageCategoriesScreen
import com.billiardsdraw.vinceproshop.presentation.admin.manage.crosssell.AdminManageCrossSellScreen
import com.billiardsdraw.vinceproshop.presentation.admin.manage.featured.AdminManageFeaturedScreen
import com.billiardsdraw.vinceproshop.presentation.admin.manage.inventory.AdminManageInventoryScreen
import com.billiardsdraw.vinceproshop.presentation.admin.manage.sizes.AdminManageSizesScreen
import com.billiardsdraw.vinceproshop.presentation.admin.navigation.AdminRoutes
import com.billiardsdraw.vinceproshop.presentation.admin.outofstock.AdminOutOfStockInterestedScreen
import com.billiardsdraw.vinceproshop.presentation.admin.orders.AdminOrdersScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AdminPanelScreen(
    currentRoute: String,
    languageCode: String,
    onSelectRoute: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdminPanelViewModel = koinViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    LaunchedEffect(currentRoute) {
        viewModel.syncCurrentRoute(currentRoute)
    }
    val activeRoute = state.currentRoute.ifBlank { currentRoute }

    AdminShell(
        currentRoute = activeRoute,
        onSelectRoute = onSelectRoute,
        onBack = onBack,
        modifier = modifier,
    ) {
        when (activeRoute) {
            AdminRoutes.Orders -> AdminOrdersScreen()
            AdminRoutes.Inventory -> AdminInventoryScreen()
            AdminRoutes.OutOfStockInterested -> AdminOutOfStockInterestedScreen()
            AdminRoutes.ManageInventory -> AdminManageInventoryScreen(languageCode = languageCode)
            AdminRoutes.ManageCategories -> AdminManageCategoriesScreen(languageCode = languageCode)
            AdminRoutes.ManageCrossSell -> AdminManageCrossSellScreen(languageCode = languageCode)
            AdminRoutes.ManageSizes -> AdminManageSizesScreen()
            AdminRoutes.ManageFeatured -> AdminManageFeaturedScreen(languageCode = languageCode)
            else -> AdminOrdersScreen()
        }
    }
}
