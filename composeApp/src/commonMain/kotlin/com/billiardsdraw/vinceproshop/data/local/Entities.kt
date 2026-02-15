package com.billiardsdraw.vinceproshop.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products_cache")
data class ProductEntity(
    @PrimaryKey val slug: String,
    val payloadJson: String,
    val name: String,
    val nameEs: String,
    val vendor: String,
    val categoryId: String,
    val price: Double,
    val imageUrl: String,
    val inStock: Int,
    val updatedAtEpochMs: Long,
)

@Entity(tableName = "categories_cache")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val nameEs: String,
    val imageUrl: String,
    val parentId: String?,
    val isCue: Boolean,
)

@Entity(tableName = "featured_cache")
data class FeaturedSlideEntity(
    @PrimaryKey val id: Int,
    val payloadJson: String,
    val sortOrder: Int,
    val isActive: Boolean,
)

@Entity(tableName = "cart_items", primaryKeys = ["slug", "size"])
data class CartItemEntity(
    val slug: String,
    val size: String,
    val quantity: Int,
    val name: String,
    val nameEs: String,
    val imageUrl: String,
    val price: Double,
    val discount: Double,
    val originalPrice: Double,
)
