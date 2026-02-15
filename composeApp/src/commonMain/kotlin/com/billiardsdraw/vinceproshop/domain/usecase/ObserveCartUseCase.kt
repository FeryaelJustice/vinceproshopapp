package com.billiardsdraw.vinceproshop.domain.usecase

import com.billiardsdraw.vinceproshop.domain.model.CartItem
import com.billiardsdraw.vinceproshop.domain.model.finalPrice
import com.billiardsdraw.vinceproshop.domain.repository.CartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class CartSnapshot(
    val items: List<CartItem>,
    val subtotal: Double,
    val totalItems: Int,
)

class ObserveCartUseCase(
    private val repository: CartRepository,
) {
    operator fun invoke(): Flow<CartSnapshot> =
        repository.observeItems().map { items ->
            val subtotal = items.sumOf { it.finalPrice * it.quantity }
            val totalItems = items.sumOf { it.quantity }
            CartSnapshot(
                items = items,
                subtotal = subtotal,
                totalItems = totalItems,
            )
        }
}
