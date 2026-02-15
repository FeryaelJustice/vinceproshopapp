package com.billiardsdraw.vinceproshop.data.repository

import com.billiardsdraw.vinceproshop.core.DispatchersProvider
import com.billiardsdraw.vinceproshop.data.remote.AccountApi
import com.billiardsdraw.vinceproshop.domain.model.AccountUser
import com.billiardsdraw.vinceproshop.domain.model.AuthSession
import com.billiardsdraw.vinceproshop.domain.model.Order
import com.billiardsdraw.vinceproshop.domain.model.OrderItem
import com.billiardsdraw.vinceproshop.domain.repository.AccountRepository
import kotlinx.coroutines.withContext

class AccountRepositoryImpl(
    private val api: AccountApi,
    private val dispatchers: DispatchersProvider,
) : AccountRepository {
    override suspend fun login(
        identifier: String,
        password: String,
    ): AuthSession =
        withContext(dispatchers.io) {
            api.login(identifier, password)
            mapSession(api.me())
        }

    override suspend fun logout() {
        withContext(dispatchers.io) {
            api.logout()
        }
    }

    override suspend fun refreshSession(): AuthSession =
        withContext(dispatchers.io) {
            runCatching { mapSession(api.me()) }
                .getOrElse { AuthSession(isAuthenticated = false, user = null) }
        }

    override suspend fun getUserOrders(): List<Order> =
        withContext(dispatchers.io) {
            api.userOrders().map { order ->
                Order(
                    id = order.id,
                    transactionId = order.transaction_id.orEmpty(),
                    status = order.status,
                    total = order.total,
                    address = order.address,
                    country = order.country,
                    customerEmail = order.customer_email,
                    customerName = order.customer_name,
                    createdAt = order.created_at,
                    items =
                        order.items.map { item ->
                            OrderItem(
                                quantity = item.quantity,
                                price = item.price,
                                size = item.size,
                                productName = item.product_name,
                            )
                        },
                )
            }
        }

    override suspend fun getAdminOrders(): List<Order> =
        withContext(dispatchers.io) {
            api.adminOrders().map { order ->
                Order(
                    id = order.id,
                    transactionId = order.transaction_id.orEmpty(),
                    status = order.status,
                    total = order.total,
                    address = order.address,
                    country = order.country,
                    customerEmail = order.customer_email,
                    customerName = order.customer_name,
                    createdAt = order.created_at,
                    items =
                        order.items.map { item ->
                            OrderItem(
                                quantity = item.quantity,
                                price = item.price,
                                size = item.size,
                                productName = item.product_name,
                            )
                        },
                )
            }
        }

    private fun mapSession(session: com.billiardsdraw.vinceproshop.data.remote.AuthSessionDto): AuthSession {
        val user =
            session.user?.let {
                AccountUser(
                    id = it.id,
                    role = it.role,
                    email = it.email,
                    username = it.username,
                )
            }
        return AuthSession(
            isAuthenticated = session.isAuthenticated && user != null,
            user = user,
        )
    }
}
