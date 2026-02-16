package com.billiardsdraw.vinceproshop.di

import com.billiardsdraw.vinceproshop.core.DispatchersProvider
import com.billiardsdraw.vinceproshop.core.StandardDispatchers
import com.billiardsdraw.vinceproshop.core.localHttpLogsEnabled
import com.billiardsdraw.vinceproshop.data.local.VinceProShopDatabase
import com.billiardsdraw.vinceproshop.data.local.createRoomDatabase
import com.billiardsdraw.vinceproshop.data.remote.AccountApi
import com.billiardsdraw.vinceproshop.data.remote.AdminApi
import com.billiardsdraw.vinceproshop.data.remote.CatalogApi
import com.billiardsdraw.vinceproshop.data.remote.ChatbotApi
import com.billiardsdraw.vinceproshop.data.remote.CheckoutApi
import com.billiardsdraw.vinceproshop.data.remote.KtorAccountApi
import com.billiardsdraw.vinceproshop.data.remote.KtorAdminApi
import com.billiardsdraw.vinceproshop.data.remote.KtorCatalogApi
import com.billiardsdraw.vinceproshop.data.remote.KtorChatbotApi
import com.billiardsdraw.vinceproshop.data.remote.KtorCheckoutApi
import com.billiardsdraw.vinceproshop.data.remote.createPlatformHttpClient
import com.billiardsdraw.vinceproshop.data.remote.defaultApiBaseUrl
import com.billiardsdraw.vinceproshop.data.repository.AccountRepositoryImpl
import com.billiardsdraw.vinceproshop.data.repository.CartRepositoryImpl
import com.billiardsdraw.vinceproshop.data.repository.CatalogRepositoryImpl
import com.billiardsdraw.vinceproshop.data.repository.ChatbotRepositoryImpl
import com.billiardsdraw.vinceproshop.data.repository.CheckoutRepositoryImpl
import com.billiardsdraw.vinceproshop.data.security.LoginCredentialStore
import com.billiardsdraw.vinceproshop.data.security.provideLoginCredentialStore
import com.billiardsdraw.vinceproshop.domain.repository.AccountRepository
import com.billiardsdraw.vinceproshop.domain.repository.CartRepository
import com.billiardsdraw.vinceproshop.domain.repository.CatalogRepository
import com.billiardsdraw.vinceproshop.domain.repository.ChatbotRepository
import com.billiardsdraw.vinceproshop.domain.repository.CheckoutRepository
import com.billiardsdraw.vinceproshop.domain.usecase.AddCartItemUseCase
import com.billiardsdraw.vinceproshop.domain.usecase.CloseChatbotSessionUseCase
import com.billiardsdraw.vinceproshop.domain.usecase.ClearCartUseCase
import com.billiardsdraw.vinceproshop.domain.usecase.CreateChatbotSessionUseCase
import com.billiardsdraw.vinceproshop.domain.usecase.CreatePaymentIntentUseCase
import com.billiardsdraw.vinceproshop.domain.usecase.GetAdminOrdersUseCase
import com.billiardsdraw.vinceproshop.domain.usecase.GetUserOrdersUseCase
import com.billiardsdraw.vinceproshop.domain.usecase.LoginUseCase
import com.billiardsdraw.vinceproshop.domain.usecase.LogoutUseCase
import com.billiardsdraw.vinceproshop.domain.usecase.ObserveCartUseCase
import com.billiardsdraw.vinceproshop.domain.usecase.ObserveCatalogUseCase
import com.billiardsdraw.vinceproshop.domain.usecase.ObserveHomeFeedUseCase
import com.billiardsdraw.vinceproshop.domain.usecase.ObserveProductDetailUseCase
import com.billiardsdraw.vinceproshop.domain.usecase.RefreshCatalogUseCase
import com.billiardsdraw.vinceproshop.domain.usecase.RefreshProductUseCase
import com.billiardsdraw.vinceproshop.domain.usecase.RefreshSessionUseCase
import com.billiardsdraw.vinceproshop.domain.usecase.RemoveCartItemUseCase
import com.billiardsdraw.vinceproshop.domain.usecase.SearchProductsUseCase
import com.billiardsdraw.vinceproshop.domain.usecase.SendChatbotMessageUseCase
import com.billiardsdraw.vinceproshop.domain.usecase.UpdateCartQuantityUseCase
import com.billiardsdraw.vinceproshop.presentation.account.AccountViewModel
import com.billiardsdraw.vinceproshop.presentation.admin.AdminPanelViewModel
import com.billiardsdraw.vinceproshop.presentation.admin.inventory.AdminInventoryViewModel
import com.billiardsdraw.vinceproshop.presentation.admin.manage.categories.AdminManageCategoriesViewModel
import com.billiardsdraw.vinceproshop.presentation.admin.manage.crosssell.AdminManageCrossSellViewModel
import com.billiardsdraw.vinceproshop.presentation.admin.manage.featured.AdminManageFeaturedViewModel
import com.billiardsdraw.vinceproshop.presentation.admin.manage.inventory.AdminManageInventoryViewModel
import com.billiardsdraw.vinceproshop.presentation.admin.manage.sizes.AdminManageSizesViewModel
import com.billiardsdraw.vinceproshop.presentation.admin.orders.AdminOrdersViewModel
import com.billiardsdraw.vinceproshop.presentation.admin.outofstock.AdminOutOfStockInterestedViewModel
import com.billiardsdraw.vinceproshop.presentation.cart.CartViewModel
import com.billiardsdraw.vinceproshop.presentation.catalog.CatalogViewModel
import com.billiardsdraw.vinceproshop.presentation.chatbot.ChatbotViewModel
import com.billiardsdraw.vinceproshop.presentation.home.HomeViewModel
import com.billiardsdraw.vinceproshop.presentation.product.ProductDetailViewModel
import com.billiardsdraw.vinceproshop.presentation.search.SearchViewModel
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

expect val platformModule: Module

val appModule =
    module {
        includes(platformModule)
        single<Json> {
            Json {
                prettyPrint = true
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            }
        }
        single<DispatchersProvider> { StandardDispatchers() }
        single<String> { defaultApiBaseUrl() }
        single<LoginCredentialStore> { provideLoginCredentialStore() }

        single { createPlatformHttpClient(get(), get(), localHttpLogsEnabled()) }
        single<VinceProShopDatabase> { createRoomDatabase() }

        single { get<VinceProShopDatabase>().productDao() }
        single { get<VinceProShopDatabase>().categoryDao() }
        single { get<VinceProShopDatabase>().featuredDao() }
        single { get<VinceProShopDatabase>().cartDao() }

        single<CatalogApi> { KtorCatalogApi(get(), get()) }
        single<AccountApi> { KtorAccountApi(get(), get(), get(), get()) }
        single<AdminApi> { KtorAdminApi(get(), get(), get(), get()) }
        single<CheckoutApi> { KtorCheckoutApi(get(), get(), get()) }
        single<ChatbotApi> { KtorChatbotApi(get(), get(), get()) }

        single<CatalogRepository> {
            CatalogRepositoryImpl(
                api = get(),
                productDao = get(),
                categoryDao = get(),
                featuredDao = get(),
                json = get(),
                dispatchers = get(),
                apiBaseUrl = get(),
            )
        }
        single<CartRepository> { CartRepositoryImpl(get(), get()) }
        single<AccountRepository> { AccountRepositoryImpl(get(), get()) }
        single<CheckoutRepository> { CheckoutRepositoryImpl(get(), get()) }
        single<ChatbotRepository> { ChatbotRepositoryImpl(get(), get()) }

        factory { RefreshCatalogUseCase(get()) }
        factory { ObserveHomeFeedUseCase(get()) }
        factory { ObserveCatalogUseCase(get()) }
        factory { SearchProductsUseCase() }
        factory { ObserveProductDetailUseCase(get()) }
        factory { RefreshProductUseCase(get()) }
        factory { ObserveCartUseCase(get()) }
        factory { AddCartItemUseCase(get()) }
        factory { UpdateCartQuantityUseCase(get()) }
        factory { RemoveCartItemUseCase(get()) }
        factory { ClearCartUseCase(get()) }
        factory { RefreshSessionUseCase(get()) }
        factory { LoginUseCase(get()) }
        factory { LogoutUseCase(get()) }
        factory { GetUserOrdersUseCase(get()) }
        factory { GetAdminOrdersUseCase(get()) }
        factory { CreatePaymentIntentUseCase(get()) }
        factory { CreateChatbotSessionUseCase(get()) }
        factory { SendChatbotMessageUseCase(get()) }
        factory { CloseChatbotSessionUseCase(get()) }

        viewModel { HomeViewModel(get(), get(), get()) }
        viewModel { CatalogViewModel(get(), get(), get()) }
        viewModel { SearchViewModel(get(), get(), get()) }
        viewModel { CartViewModel(get(), get(), get(), get(), get(), get()) }
        viewModel { AccountViewModel(get(), get(), get(), get(), get(), get(), get()) }
        viewModel { (slug: String) -> ProductDetailViewModel(slug, get(), get(), get(), get()) }
        viewModel { AdminPanelViewModel() }
        viewModel { AdminOrdersViewModel(get(), get()) }
        viewModel { AdminInventoryViewModel(get(), get()) }
        viewModel { AdminOutOfStockInterestedViewModel(get(), get()) }
        viewModel { AdminManageInventoryViewModel(get(), get()) }
        viewModel { AdminManageCategoriesViewModel(get(), get()) }
        viewModel { AdminManageCrossSellViewModel(get(), get(), get()) }
        viewModel { AdminManageSizesViewModel(get(), get()) }
        viewModel { AdminManageFeaturedViewModel(get(), get(), get()) }
        viewModel { ChatbotViewModel(get(), get(), get(), get()) }
    }
