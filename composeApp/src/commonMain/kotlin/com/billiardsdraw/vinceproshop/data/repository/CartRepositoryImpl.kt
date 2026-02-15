package com.billiardsdraw.vinceproshop.data.repository

import com.billiardsdraw.vinceproshop.core.DispatchersProvider
import com.billiardsdraw.vinceproshop.data.local.CartDao
import com.billiardsdraw.vinceproshop.data.mapper.toDomain
import com.billiardsdraw.vinceproshop.data.mapper.toEntity
import com.billiardsdraw.vinceproshop.domain.model.CartItem
import com.billiardsdraw.vinceproshop.domain.repository.CartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CartRepositoryImpl(
    private val cartDao: CartDao,
    private val dispatchers: DispatchersProvider,
) : CartRepository {
    override fun observeItems(): Flow<List<CartItem>> =
        cartDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun addOrMerge(item: CartItem) {
        withContext(dispatchers.io) {
            val existing = cartDao.findByKey(item.slug, item.size)
            if (existing == null) {
                cartDao.upsert(item.toEntity())
            } else {
                cartDao.updateQuantity(item.slug, item.size, existing.quantity + item.quantity)
            }
        }
    }

    override suspend fun updateQuantity(
        slug: String,
        size: String,
        quantity: Int,
    ) {
        withContext(dispatchers.io) {
            cartDao.updateQuantity(slug, size, quantity)
        }
    }

    override suspend fun remove(
        slug: String,
        size: String,
    ) {
        withContext(dispatchers.io) {
            cartDao.remove(slug, size)
        }
    }

    override suspend fun clear() {
        withContext(dispatchers.io) {
            cartDao.clear()
        }
    }
}
