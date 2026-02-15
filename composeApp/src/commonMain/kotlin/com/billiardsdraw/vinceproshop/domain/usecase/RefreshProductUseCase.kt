package com.billiardsdraw.vinceproshop.domain.usecase

import com.billiardsdraw.vinceproshop.domain.repository.CatalogRepository

class RefreshProductUseCase(
    private val repository: CatalogRepository,
) {
    suspend operator fun invoke(slug: String) = repository.refreshProduct(slug)
}
