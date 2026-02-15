package com.billiardsdraw.vinceproshop.domain.usecase

import com.billiardsdraw.vinceproshop.domain.model.FeaturedSlide
import com.billiardsdraw.vinceproshop.domain.model.Product
import com.billiardsdraw.vinceproshop.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class HomeFeed(
    val featured: List<FeaturedSlide>,
    val quickView: List<Product>,
)

class ObserveHomeFeedUseCase(
    private val repository: CatalogRepository,
) {
    operator fun invoke(): Flow<HomeFeed> {
        return combine(
            repository.observeFeaturedSlides(),
            repository.observeProducts(),
        ) { featured, products ->
            HomeFeed(
                featured = featured.sortedBy { it.sortOrder },
                quickView = products.take(3),
            )
        }
    }
}
