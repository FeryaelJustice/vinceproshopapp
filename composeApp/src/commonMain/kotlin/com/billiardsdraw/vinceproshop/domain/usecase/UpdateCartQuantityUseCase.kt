package com.billiardsdraw.vinceproshop.domain.usecase

import com.billiardsdraw.vinceproshop.domain.repository.CartRepository

class UpdateCartQuantityUseCase(
    private val repository: CartRepository,
) {
    suspend operator fun invoke(
        slug: String,
        size: String,
        quantity: Int,
    ) {
        if (quantity <= 0) {
            repository.remove(slug, size)
            return
        }
        repository.updateQuantity(slug, size, quantity)
    }
}
