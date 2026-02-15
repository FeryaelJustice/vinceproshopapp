package com.billiardsdraw.vinceproshop.presentation.admin.navigation

data class AdminDestinationItem(
    val route: String,
    val label: String,
    val section: AdminSection,
)

enum class AdminSection {
    Overview,
    Manage,
}

object AdminRoutes {
    const val Orders = "/admin/orders"
    const val Inventory = "/admin/inventory"
    const val OutOfStockInterested = "/admin/out-of-stock-interested"
    const val ManageInventory = "/admin/inventory/manage"
    const val ManageCategories = "/admin/categories/manage"
    const val ManageCrossSell = "/admin/cross-sell/manage"
    const val ManageSizes = "/admin/sizes/manage"
    const val ManageFeatured = "/admin/featured/manage"
}

val adminDestinations: List<AdminDestinationItem> =
    listOf(
        AdminDestinationItem(AdminRoutes.Orders, "Orders", AdminSection.Overview),
        AdminDestinationItem(AdminRoutes.Inventory, "Inventory", AdminSection.Overview),
        AdminDestinationItem(AdminRoutes.OutOfStockInterested, "Out Of Stock", AdminSection.Overview),
        AdminDestinationItem(AdminRoutes.ManageInventory, "Manage Inventory", AdminSection.Manage),
        AdminDestinationItem(AdminRoutes.ManageCategories, "Manage Categories", AdminSection.Manage),
        AdminDestinationItem(AdminRoutes.ManageCrossSell, "Manage Cross Sell", AdminSection.Manage),
        AdminDestinationItem(AdminRoutes.ManageSizes, "Manage Sizes", AdminSection.Manage),
        AdminDestinationItem(AdminRoutes.ManageFeatured, "Manage Featured", AdminSection.Manage),
    )

fun adminTitle(route: String): String = adminDestinations.firstOrNull { it.route == route }?.label ?: route
