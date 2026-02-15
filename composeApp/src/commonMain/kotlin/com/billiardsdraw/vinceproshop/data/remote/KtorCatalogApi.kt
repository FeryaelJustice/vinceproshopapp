package com.billiardsdraw.vinceproshop.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class KtorCatalogApi(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) : CatalogApi {

    override suspend fun getProducts(): List<ProductDto> {
        return httpClient.get(url("products")).body()
    }

    override suspend fun getCategories(): List<CategoryDto> {
        return httpClient.get(url("products/categories")).body()
    }

    override suspend fun getFeaturedSlides(): List<FeaturedSlideDto> {
        return httpClient.get(url("products/featured")).body()
    }

    override suspend fun getProductBySlug(slug: String): ProductDto? {
        return runCatching {
            httpClient.get(url("products/$slug")).body<ProductDto>()
        }.getOrNull()
    }

    private fun url(path: String): String = "${baseUrl.trimEnd('/')}/$path"
}
