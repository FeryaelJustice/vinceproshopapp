package com.billiardsdraw.vinceproshop.domain.usecase

import com.billiardsdraw.vinceproshop.domain.model.Product

class SearchProductsUseCase {
    operator fun invoke(products: List<Product>, query: String): List<Product> {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) return emptyList()
        return products.filter { product ->
            product.name.lowercase().contains(normalized) ||
                product.nameEs.lowercase().contains(normalized) ||
                product.vendor.lowercase().contains(normalized)
        }
    }
}
