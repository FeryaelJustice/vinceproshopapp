package com.billiardsdraw.vinceproshop.domain.repository

import com.billiardsdraw.vinceproshop.domain.model.Category
import com.billiardsdraw.vinceproshop.domain.model.FeaturedSlide
import com.billiardsdraw.vinceproshop.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface CatalogRepository {
    fun observeProducts(): Flow<List<Product>>

    fun observeCategories(): Flow<List<Category>>

    fun observeFeaturedSlides(): Flow<List<FeaturedSlide>>

    fun observeProduct(slug: String): Flow<Product?>

    suspend fun refreshCatalog()

    suspend fun refreshProduct(slug: String)
}
