package com.billiardsdraw.vinceproshop.domain.repository

import com.billiardsdraw.vinceproshop.domain.model.CartItem
import kotlinx.coroutines.flow.Flow

interface CartRepository {
    fun observeItems(): Flow<List<CartItem>>
    suspend fun addOrMerge(item: CartItem)
    suspend fun updateQuantity(slug: String, size: String, quantity: Int)
    suspend fun remove(slug: String, size: String)
    suspend fun clear()
}
