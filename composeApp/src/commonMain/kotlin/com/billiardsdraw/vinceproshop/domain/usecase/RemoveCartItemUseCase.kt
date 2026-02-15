package com.billiardsdraw.vinceproshop.domain.usecase

import com.billiardsdraw.vinceproshop.domain.repository.CartRepository

class RemoveCartItemUseCase(
    private val repository: CartRepository,
) {
    suspend operator fun invoke(slug: String, size: String) = repository.remove(slug, size)
}
