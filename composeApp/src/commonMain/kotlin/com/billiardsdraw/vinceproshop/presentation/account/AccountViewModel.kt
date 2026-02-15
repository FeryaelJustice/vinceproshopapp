package com.billiardsdraw.vinceproshop.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billiardsdraw.vinceproshop.core.DispatchersProvider
import com.billiardsdraw.vinceproshop.data.security.LoginCredential
import com.billiardsdraw.vinceproshop.data.security.LoginCredentialStore
import com.billiardsdraw.vinceproshop.domain.model.AccountUser
import com.billiardsdraw.vinceproshop.domain.model.Order
import com.billiardsdraw.vinceproshop.domain.usecase.GetAdminOrdersUseCase
import com.billiardsdraw.vinceproshop.domain.usecase.GetUserOrdersUseCase
import com.billiardsdraw.vinceproshop.domain.usecase.LoginUseCase
import com.billiardsdraw.vinceproshop.domain.usecase.LogoutUseCase
import com.billiardsdraw.vinceproshop.domain.usecase.RefreshSessionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminPanelSection(
    val label: String,
    val route: String,
)

data class AccountUiState(
    val isLoadingSession: Boolean = true,
    val isSubmittingAuth: Boolean = false,
    val isLoadingOrders: Boolean = false,
    val user: AccountUser? = null,
    val authError: String? = null,
    val ordersError: String? = null,
    val savedCredential: LoginCredential? = null,
    val userOrders: List<Order> = emptyList(),
    val adminOrders: List<Order> = emptyList(),
    val adminSections: List<AdminPanelSection> = defaultAdminSections(),
)

class AccountViewModel(
    private val refreshSession: RefreshSessionUseCase,
    private val loginUseCase: LoginUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val getUserOrders: GetUserOrdersUseCase,
    private val getAdminOrders: GetAdminOrdersUseCase,
    private val dispatchers: DispatchersProvider,
    private val loginCredentialStore: LoginCredentialStore,
) : ViewModel() {

    private val _state = MutableStateFlow(AccountUiState())
    val state: StateFlow<AccountUiState> = _state.asStateFlow()

    init {
        refresh()
        loadSavedCredential()
    }

    fun refresh() {
        _state.value = _state.value.copy(
            isLoadingSession = true,
            authError = null,
            ordersError = null,
        )

        viewModelScope.launch(dispatchers.io) {
            val session = refreshSession()
            if (!session.isAuthenticated || session.user == null) {
                _state.value = AccountUiState(
                    isLoadingSession = false,
                    savedCredential = _state.value.savedCredential,
                    adminSections = defaultAdminSections(),
                )
                return@launch
            }
            loadOrdersFor(session.user)
        }
    }

    fun login(identifier: String, password: String) {
        if (identifier.isBlank() || password.isBlank()) {
            _state.value = _state.value.copy(authError = "Missing credentials")
            return
        }

        _state.value = _state.value.copy(
            isSubmittingAuth = true,
            authError = null,
            ordersError = null,
        )

        viewModelScope.launch(dispatchers.io) {
            runCatching {
                loginUseCase(identifier.trim(), password)
            }.onSuccess { session ->
                runCatching {
                    loginCredentialStore.saveCredential(identifier.trim(), password)
                }
                if (session.user == null) {
                    _state.value = _state.value.copy(
                        isSubmittingAuth = false,
                        authError = "Could not load session",
                    )
                    return@onSuccess
                }
                loadOrdersFor(session.user, keepLoadingFlag = false)
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    isSubmittingAuth = false,
                    authError = error.message ?: "Login failed",
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch(dispatchers.io) {
            runCatching { logoutUseCase() }
            _state.value = AccountUiState(
                isLoadingSession = false,
                savedCredential = _state.value.savedCredential,
                adminSections = defaultAdminSections(),
            )
        }
    }

    fun clearAuthError() {
        _state.value = _state.value.copy(authError = null)
    }

    fun clearSavedCredential() {
        viewModelScope.launch(dispatchers.io) {
            runCatching { loginCredentialStore.clearCredential() }
            _state.value = _state.value.copy(savedCredential = null)
        }
    }

    private fun loadSavedCredential() {
        viewModelScope.launch(dispatchers.io) {
            val credential = runCatching { loginCredentialStore.readCredential() }.getOrNull()
            _state.value = _state.value.copy(savedCredential = credential)
        }
    }

    private suspend fun loadOrdersFor(user: AccountUser, keepLoadingFlag: Boolean = true) {
        if (keepLoadingFlag) {
            _state.value = _state.value.copy(
                isLoadingSession = false,
                isLoadingOrders = true,
                user = user,
                authError = null,
                ordersError = null,
            )
        } else {
            _state.value = _state.value.copy(
                isLoadingSession = false,
                isSubmittingAuth = false,
                isLoadingOrders = true,
                user = user,
                authError = null,
                ordersError = null,
            )
        }

        runCatching {
            if (user.role.equals("admin", ignoreCase = true)) getAdminOrders() else getUserOrders()
        }.onSuccess { orders ->
            if (user.role.equals("admin", ignoreCase = true)) {
                _state.value = _state.value.copy(
                    isLoadingOrders = false,
                    userOrders = emptyList(),
                    adminOrders = orders,
                )
            } else {
                _state.value = _state.value.copy(
                    isLoadingOrders = false,
                    userOrders = orders,
                    adminOrders = emptyList(),
                )
            }
        }.onFailure { error ->
            _state.value = _state.value.copy(
                isLoadingOrders = false,
                ordersError = error.message ?: "Could not load orders",
            )
        }
    }
}

private fun defaultAdminSections(): List<AdminPanelSection> = listOf(
    AdminPanelSection(label = "Orders", route = "/admin/orders"),
    AdminPanelSection(label = "Inventory", route = "/admin/inventory"),
    AdminPanelSection(label = "Out Of Stock", route = "/admin/out-of-stock-interested"),
    AdminPanelSection(label = "Manage Inventory", route = "/admin/inventory/manage"),
    AdminPanelSection(label = "Manage Categories", route = "/admin/categories/manage"),
    AdminPanelSection(label = "Manage Cross Sell", route = "/admin/cross-sell/manage"),
    AdminPanelSection(label = "Manage Sizes", route = "/admin/sizes/manage"),
    AdminPanelSection(label = "Manage Featured", route = "/admin/featured/manage"),
)
