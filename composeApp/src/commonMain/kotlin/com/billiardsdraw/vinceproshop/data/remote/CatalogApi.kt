package com.billiardsdraw.vinceproshop.data.remote

interface CatalogApi {
    suspend fun getProducts(): List<ProductDto>

    suspend fun getCategories(): List<CategoryDto>

    suspend fun getFeaturedSlides(): List<FeaturedSlideDto>

    suspend fun getProductBySlug(slug: String): ProductDto?
}
