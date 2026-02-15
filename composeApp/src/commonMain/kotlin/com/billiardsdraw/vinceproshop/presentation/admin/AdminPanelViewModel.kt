package com.billiardsdraw.vinceproshop.presentation.admin

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AdminPanelUiState(
    val currentRoute: String = "",
)

class AdminPanelViewModel : ViewModel() {
    private val _state = MutableStateFlow(AdminPanelUiState())
    val state: StateFlow<AdminPanelUiState> = _state.asStateFlow()

    fun syncCurrentRoute(route: String) {
        if (_state.value.currentRoute == route) return
        _state.value = _state.value.copy(currentRoute = route)
    }
}
