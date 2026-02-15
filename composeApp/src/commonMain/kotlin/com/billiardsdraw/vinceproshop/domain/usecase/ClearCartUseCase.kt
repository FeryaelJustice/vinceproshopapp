package com.billiardsdraw.vinceproshop.domain.usecase

import com.billiardsdraw.vinceproshop.domain.repository.CartRepository

class ClearCartUseCase(
    private val repository: CartRepository,
) {
    suspend operator fun invoke() = repository.clear()
}
