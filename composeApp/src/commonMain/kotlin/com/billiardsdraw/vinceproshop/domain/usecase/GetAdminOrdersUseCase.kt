package com.billiardsdraw.vinceproshop.domain.usecase

import com.billiardsdraw.vinceproshop.domain.model.Order
import com.billiardsdraw.vinceproshop.domain.repository.AccountRepository

class GetAdminOrdersUseCase(
    private val repository: AccountRepository,
) {
    suspend operator fun invoke(): List<Order> {
        return repository.getAdminOrders()
    }
}
