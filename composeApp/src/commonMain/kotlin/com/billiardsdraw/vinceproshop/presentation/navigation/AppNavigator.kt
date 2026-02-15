package com.billiardsdraw.vinceproshop.presentation.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class RootSection {
    Home,
    Catalog,
    Search,
    Cart,
}

sealed interface AppDestination {
    data class Root(
        val section: RootSection,
    ) : AppDestination

    data class ProductDetail(
        val slug: String,
        val source: RootSection,
    ) : AppDestination

    data class AdminRoute(
        val route: String,
        val source: RootSection,
    ) : AppDestination
}

class AppNavigator {
    private val backstack = mutableStateListOf<AppDestination>(AppDestination.Root(RootSection.Home))

    var current: AppDestination by mutableStateOf(backstack.last())
        private set

    fun openRoot(section: RootSection) {
        backstack.clear()
        backstack += AppDestination.Root(section)
        current = backstack.last()
    }

    fun openProduct(slug: String) {
        val source =
            when (val destination = current) {
                is AppDestination.Root -> destination.section
                is AppDestination.ProductDetail -> destination.source
                is AppDestination.AdminRoute -> destination.source
            }
        backstack += AppDestination.ProductDetail(slug, source)
        current = backstack.last()
    }

    fun openAdmin(route: String) {
        val source =
            when (val destination = current) {
                is AppDestination.Root -> destination.section
                is AppDestination.ProductDetail -> destination.source
                is AppDestination.AdminRoute -> destination.source
            }
        backstack += AppDestination.AdminRoute(route = route, source = source)
        current = backstack.last()
    }

    fun switchAdmin(route: String) {
        val currentDestination = current
        if (currentDestination is AppDestination.AdminRoute) {
            backstack[backstack.lastIndex] = currentDestination.copy(route = route)
            current = backstack.last()
            return
        }
        openAdmin(route)
    }

    fun back() {
        if (backstack.size > 1) {
            backstack.removeLast()
            current = backstack.last()
        }
    }

    fun selectedSection(): RootSection =
        when (val destination = current) {
            is AppDestination.Root -> destination.section
            is AppDestination.ProductDetail -> destination.source
            is AppDestination.AdminRoute -> destination.source
        }
}
