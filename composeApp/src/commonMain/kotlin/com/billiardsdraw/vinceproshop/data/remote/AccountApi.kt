package com.billiardsdraw.vinceproshop.data.remote

interface AccountApi {
    suspend fun login(
        identifier: String,
        password: String,
    ): LoginResponseDto

    suspend fun logout()

    suspend fun me(): AuthSessionDto

    suspend fun userOrders(): List<OrderDto>

    suspend fun adminOrders(): List<OrderDto>
}
