package com.billiardsdraw.vinceproshop.domain.repository

import com.billiardsdraw.vinceproshop.domain.model.AuthSession
import com.billiardsdraw.vinceproshop.domain.model.Order

interface AccountRepository {
    suspend fun login(
        identifier: String,
        password: String,
    ): AuthSession

    suspend fun logout()

    suspend fun refreshSession(): AuthSession

    suspend fun getUserOrders(): List<Order>

    suspend fun getAdminOrders(): List<Order>
}
