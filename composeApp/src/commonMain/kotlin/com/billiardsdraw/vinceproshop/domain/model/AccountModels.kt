package com.billiardsdraw.vinceproshop.domain.model

data class AccountUser(
    val id: Int,
    val role: String,
    val email: String,
    val username: String,
)

data class AuthSession(
    val isAuthenticated: Boolean,
    val user: AccountUser?,
)

data class OrderItem(
    val quantity: Int,
    val price: Double,
    val size: String,
    val productName: String,
)

data class Order(
    val id: Int,
    val transactionId: String,
    val status: String,
    val total: Double,
    val address: String,
    val country: String,
    val customerEmail: String,
    val customerName: String,
    val createdAt: String,
    val items: List<OrderItem>,
)
