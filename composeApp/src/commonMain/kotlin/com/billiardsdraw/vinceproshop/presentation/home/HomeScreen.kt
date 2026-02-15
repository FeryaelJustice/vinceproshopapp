package com.billiardsdraw.vinceproshop.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.billiardsdraw.vinceproshop.presentation.common.tr
import com.billiardsdraw.vinceproshop.presentation.components.EmptyState
import com.billiardsdraw.vinceproshop.presentation.components.HeroCarousel
import com.billiardsdraw.vinceproshop.presentation.components.ProductCard

private enum class HomeItemType {
    Hero,         // HeroCarousel
    SectionTitle, // Text "Quick View"
    Empty,        // EmptyState sin productos
    ProductRow,   // LazyRow con ProductCard
    ProductRows,  // Lazy Row con ProductCard items
    Error,        // Text de errorMessage
    RetryButton,  // Button "Reintentar"
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    languageCode: String,
    onOpenProduct: (String) -> Unit,
    onOpenCategory: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item(contentType = HomeItemType.Hero) {
            HeroCarousel(
                slides = state.featuredSlides,
                languageCode = languageCode,
                onSlideClick = { slide ->
                    when {
                        !slide.slug.isNullOrBlank() -> onOpenProduct(slide.slug)
                        !slide.categoryId.isNullOrBlank() -> onOpenCategory(slide.categoryId)
                    }
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }

        item(contentType = HomeItemType.SectionTitle) {
            Text(
                text = tr("Quick View", "Vista rapida"),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        if (state.quickViewProducts.isEmpty() && !state.isLoading) {
            item(contentType = HomeItemType.Empty) {
                EmptyState(
                    title = tr("No products available", "No hay productos disponibles"),
                    description = tr(
                        "Try refreshing to fetch the latest catalog.",
                        "Intenta actualizar para traer el catalogo mas reciente.",
                    ),
                )
            }
        }

        item(contentType = HomeItemType.ProductRow) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                items(state.quickViewProducts, key = { it.slug }, contentType = {
                    HomeItemType.ProductRows
                }) { product ->
                    ProductCard(
                        product = product,
                        languageCode = languageCode,
                        isCueLayout = false,
                        onClick = { onOpenProduct(product.slug) },
                        modifier = Modifier.fillMaxWidth(0.72f),
                    )
                }
            }
        }

        state.errorMessage?.let {
            item(contentType = HomeItemType.Error) {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item(contentType = HomeItemType.RetryButton) {
                Button(onClick = onRetry, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(text = tr("Retry", "Reintentar"))
                }
            }
        }
    }
}
