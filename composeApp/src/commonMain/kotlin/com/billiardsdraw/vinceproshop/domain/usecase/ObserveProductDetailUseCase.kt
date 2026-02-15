package com.billiardsdraw.vinceproshop.domain.usecase

import com.billiardsdraw.vinceproshop.domain.model.Product
import com.billiardsdraw.vinceproshop.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow

class ObserveProductDetailUseCase(
    private val repository: CatalogRepository,
) {
    operator fun invoke(slug: String): Flow<Product?> = repository.observeProduct(slug)
}
