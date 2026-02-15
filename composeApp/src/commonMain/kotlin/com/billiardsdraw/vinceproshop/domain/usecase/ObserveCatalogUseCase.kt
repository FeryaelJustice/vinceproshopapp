package com.billiardsdraw.vinceproshop.domain.usecase

import com.billiardsdraw.vinceproshop.domain.model.Category
import com.billiardsdraw.vinceproshop.domain.model.Product
import com.billiardsdraw.vinceproshop.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class CatalogFeed(
    val products: List<Product>,
    val categories: List<Category>,
)

class ObserveCatalogUseCase(
    private val repository: CatalogRepository,
) {
    operator fun invoke(): Flow<CatalogFeed> =
        combine(
            repository.observeProducts(),
            repository.observeCategories(),
        ) { products, categories ->
            CatalogFeed(products = products, categories = categories)
        }
}
