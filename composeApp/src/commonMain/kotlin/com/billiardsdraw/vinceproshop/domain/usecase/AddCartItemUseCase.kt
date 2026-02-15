package com.billiardsdraw.vinceproshop.domain.usecase

import com.billiardsdraw.vinceproshop.domain.model.CartItem
import com.billiardsdraw.vinceproshop.domain.repository.CartRepository

class AddCartItemUseCase(
    private val repository: CartRepository,
) {
    suspend operator fun invoke(item: CartItem) = repository.addOrMerge(item)
}
