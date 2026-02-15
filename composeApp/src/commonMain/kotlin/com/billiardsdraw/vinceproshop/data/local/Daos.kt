package com.billiardsdraw.vinceproshop.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products_cache ORDER BY name ASC")
    fun observeAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products_cache WHERE slug = :slug LIMIT 1")
    fun observeBySlug(slug: String): Flow<ProductEntity?>

    @Upsert
    suspend fun upsertAll(products: List<ProductEntity>)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories_cache ORDER BY name ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Upsert
    suspend fun upsertAll(categories: List<CategoryEntity>)
}

@Dao
interface FeaturedDao {
    @Query("SELECT * FROM featured_cache ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<FeaturedSlideEntity>>

    @Upsert
    suspend fun upsertAll(slides: List<FeaturedSlideEntity>)
}

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items ORDER BY name ASC")
    fun observeAll(): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items WHERE slug = :slug AND size = :size LIMIT 1")
    suspend fun findByKey(
        slug: String,
        size: String,
    ): CartItemEntity?

    @Upsert
    suspend fun upsert(item: CartItemEntity)

    @Query("UPDATE cart_items SET quantity = :quantity WHERE slug = :slug AND size = :size")
    suspend fun updateQuantity(
        slug: String,
        size: String,
        quantity: Int,
    )

    @Query("DELETE FROM cart_items WHERE slug = :slug AND size = :size")
    suspend fun remove(
        slug: String,
        size: String,
    )

    @Query("DELETE FROM cart_items")
    suspend fun clear()
}
