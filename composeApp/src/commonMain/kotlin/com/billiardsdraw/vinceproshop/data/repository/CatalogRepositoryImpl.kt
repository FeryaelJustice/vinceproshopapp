package com.billiardsdraw.vinceproshop.data.repository

import com.billiardsdraw.vinceproshop.core.DispatchersProvider
import com.billiardsdraw.vinceproshop.data.local.CategoryDao
import com.billiardsdraw.vinceproshop.data.local.FeaturedDao
import com.billiardsdraw.vinceproshop.data.local.ProductDao
import com.billiardsdraw.vinceproshop.data.mapper.toDomain
import com.billiardsdraw.vinceproshop.data.mapper.toEntity
import com.billiardsdraw.vinceproshop.data.remote.CatalogApi
import com.billiardsdraw.vinceproshop.domain.model.Category
import com.billiardsdraw.vinceproshop.domain.model.FeaturedSlide
import com.billiardsdraw.vinceproshop.domain.model.Product
import com.billiardsdraw.vinceproshop.domain.repository.CatalogRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class CatalogRepositoryImpl(
    private val api: CatalogApi,
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao,
    private val featuredDao: FeaturedDao,
    private val json: Json,
    private val dispatchers: DispatchersProvider,
    private val apiBaseUrl: String,
) : CatalogRepository {

    override fun observeProducts(): Flow<List<Product>> {
        return productDao.observeAll().map { entities ->
            entities.map { it.toDomain(json) }
        }
    }

    override fun observeCategories(): Flow<List<Category>> {
        return categoryDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeFeaturedSlides(): Flow<List<FeaturedSlide>> {
        return featuredDao.observeAll().map { entities ->
            entities.map { it.toDomain(json) }.filter { it.isActive }
        }
    }

    override fun observeProduct(slug: String): Flow<Product?> {
        return productDao.observeBySlug(slug).map { entity ->
            entity?.toDomain(json)
        }
    }

    override suspend fun refreshCatalog() {
        safeIo(dispatchers.io) {
            val categories = api.getCategories().map { it.toDomain(apiBaseUrl) }
            val products = api.getProducts().map { it.toDomain(apiBaseUrl) }
            val featured = api.getFeaturedSlides().map { it.toDomain(apiBaseUrl) }

            categoryDao.upsertAll(categories.map { it.toEntity() })
            productDao.upsertAll(products.map { it.toEntity(json) })
            featuredDao.upsertAll(featured.map { it.toEntity(json) })
        }
    }

    override suspend fun refreshProduct(slug: String) {
        safeIo(dispatchers.io) {
            val product = api.getProductBySlug(slug)?.toDomain(apiBaseUrl) ?: return@safeIo
            productDao.upsertAll(listOf(product.toEntity(json)))
        }
    }

    private suspend inline fun safeIo(
        dispatcher: CoroutineDispatcher,
        crossinline block: suspend () -> Unit,
    ) {
        runCatching {
            withContext(dispatcher) {
                block()
            }
        }
    }
}
