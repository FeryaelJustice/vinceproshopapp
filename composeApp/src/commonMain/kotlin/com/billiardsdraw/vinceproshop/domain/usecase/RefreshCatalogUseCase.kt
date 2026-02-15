package com.billiardsdraw.vinceproshop.domain.usecase

import com.billiardsdraw.vinceproshop.domain.repository.CatalogRepository

class RefreshCatalogUseCase(
    private val repository: CatalogRepository,
) {
    suspend operator fun invoke() = repository.refreshCatalog()
}
