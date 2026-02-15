package com.billiardsdraw.vinceproshop.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class FeaturedSlide(
    val id: Int,
    val targetType: String,
    val productId: Int?,
    val categoryId: String?,
    val slug: String?,
    val name: String,
    val nameEs: String,
    val imageUrl: String,
    val title: String,
    val titleEs: String,
    val subtitle: String,
    val subtitleEs: String,
    val sortOrder: Int,
    val isActive: Boolean,
)

fun FeaturedSlide.localizedTitle(languageCode: String): String {
    return if (languageCode.startsWith("es", ignoreCase = true)) titleEs.ifBlank { title } else title
}

fun FeaturedSlide.localizedSubtitle(languageCode: String): String {
    return if (languageCode.startsWith("es", ignoreCase = true)) subtitleEs.ifBlank { subtitle } else subtitle
}
