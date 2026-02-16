package com.billiardsdraw.vinceproshop.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.billiardsdraw.vinceproshop.presentation.common.tr
import com.billiardsdraw.vinceproshop.presentation.components.EmptyState
import com.billiardsdraw.vinceproshop.presentation.components.HeroCarousel
import com.billiardsdraw.vinceproshop.presentation.components.ProductCard
import com.billiardsdraw.vinceproshop.presentation.legal.LegalDocument
import org.jetbrains.compose.resources.vectorResource
import vinceproshop_app.composeapp.generated.resources.Res
import vinceproshop_app.composeapp.generated.resources.phone_call

private enum class HomeItemType {
    Hero, // HeroCarousel
    SectionTitle, // Text "Quick View"
    Empty, // EmptyState sin productos
    ProductRow, // LazyRow con ProductCard
    ProductRows, // Lazy Row con ProductCard items
    Footer, // Bloque legal estilo frontend
    Error, // Text de errorMessage
    RetryButton, // Button "Reintentar"
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    languageCode: String,
    onOpenProduct: (String) -> Unit,
    onOpenCategory: (String) -> Unit,
    onOpenLegalDocument: (LegalDocument) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current

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
                    description =
                        tr(
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
                items(
                    items = state.quickViewProducts,
                    key = { it.slug },
                    contentType = { HomeItemType.ProductRows },
                ) { product ->
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

        item(contentType = HomeItemType.Footer) {
            HomeLegalFooter(
                onOpenLegalDocument = onOpenLegalDocument,
                onOpenContact = {
                    runCatching { uriHandler.openUri("mailto:support@vinceproshop.com") }
                },
                onPhoneCall = {
                    runCatching { uriHandler.openUri("tel:+34651727480") }
                },
            )
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

@Composable
private fun HomeLegalFooter(
    onOpenLegalDocument: (LegalDocument) -> Unit,
    onOpenContact: () -> Unit,
    onPhoneCall: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BoxWithConstraints {
                val compact = maxWidth < 720.dp

                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        FooterLinksSection(
                            title = tr("Information", "Informacion"),
                            links =
                                listOf(
                                    tr("About us", "Sobre nosotros") to {
                                        onOpenLegalDocument(LegalDocument.About)
                                    },
                                    tr("Shipping", "Envios") to {
                                        onOpenLegalDocument(LegalDocument.Shipping)
                                    },
                                    tr("Returns", "Devoluciones") to {
                                        onOpenLegalDocument(LegalDocument.Returns)
                                    },
                                    tr("Contact us", "Contactanos") to onOpenContact,
                                ),
                        )

                        FooterLinksSection(
                            title = tr("Legal Pages", "Paginas Legales"),
                            links =
                                listOf(
                                    tr("Cookie Policy", "Politica de cookies") to {
                                        onOpenLegalDocument(LegalDocument.Cookies)
                                    },
                                    tr("Terms of Use", "Terminos de uso") to {
                                        onOpenLegalDocument(LegalDocument.Terms)
                                    },
                                    tr("Legal Notice", "Aviso legal") to {
                                        onOpenLegalDocument(LegalDocument.Notice)
                                    },
                                    tr("Privacy Policy", "Politica de privacidad") to {
                                        onOpenLegalDocument(LegalDocument.Privacy)
                                    },
                                ),
                        )

                        PhoneCircleButton(onClick = onPhoneCall)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        FooterLinksSection(
                            title = tr("Information", "Informacion"),
                            links =
                                listOf(
                                    tr("About us", "Sobre nosotros") to {
                                        onOpenLegalDocument(LegalDocument.About)
                                    },
                                    tr("Shipping", "Envios") to {
                                        onOpenLegalDocument(LegalDocument.Shipping)
                                    },
                                    tr("Returns", "Devoluciones") to {
                                        onOpenLegalDocument(LegalDocument.Returns)
                                    },
                                    tr("Contact us", "Contactanos") to onOpenContact,
                                ),
                            modifier = Modifier.weight(1f),
                        )

                        FooterLinksSection(
                            title = tr("Legal Pages", "Paginas Legales"),
                            links =
                                listOf(
                                    tr("Cookie Policy", "Politica de cookies") to {
                                        onOpenLegalDocument(LegalDocument.Cookies)
                                    },
                                    tr("Terms of Use", "Terminos de uso") to {
                                        onOpenLegalDocument(LegalDocument.Terms)
                                    },
                                    tr("Legal Notice", "Aviso legal") to {
                                        onOpenLegalDocument(LegalDocument.Notice)
                                    },
                                    tr("Privacy Policy", "Politica de privacidad") to {
                                        onOpenLegalDocument(LegalDocument.Privacy)
                                    },
                                ),
                            modifier = Modifier.weight(1f),
                        )

                        Box(
                            modifier = Modifier.weight(0.45f),
                            contentAlignment = Alignment.TopEnd,
                        ) {
                            PhoneCircleButton(onClick = onPhoneCall)
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

            Text(
                text = "(c) 2026 Vince Billiards Pro Shop. ${tr("All rights reserved.", "Todos los derechos reservados.")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FooterLinksSection(
    title: String,
    links: List<Pair<String, () -> Unit>>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        HorizontalDivider(
            modifier = Modifier.padding(bottom = 6.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        )

        links.forEach { (label, onClick) ->
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable(onClick = onClick),
            )
        }
    }
}

@Composable
private fun PhoneCircleButton(onClick: () -> Unit) {
    Surface(
        modifier =
            Modifier
                .size(62.dp)
                .clickable(onClick = onClick),
        shape = CircleShape,
        color = Color.Transparent,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = vectorResource(Res.drawable.phone_call),
                contentDescription = tr("Call us", "Llamanos"),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
