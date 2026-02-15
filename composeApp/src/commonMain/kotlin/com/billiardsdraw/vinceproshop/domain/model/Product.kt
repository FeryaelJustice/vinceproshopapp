package com.billiardsdraw.vinceproshop.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ProductOption(
    val label: String,
    val value: String,
    val quantity: Int,
    val price: Double,
    val discount: Double,
)

@Serializable
data class ProductMedia(
    val url: String,
)

@Serializable
data class Product(
    val id: Int,
    val slug: String,
    val name: String,
    val nameEs: String,
    val vendor: String,
    val price: Double,
    val categoryId: String,
    val description: String,
    val descriptionEs: String,
    val imageUrl: String,
    val inStock: Int,
    val maxDiscount: Double,
    val media: List<ProductMedia>,
    val options: List<ProductOption>,
)

fun Product.localizedName(languageCode: String): String =
    if (languageCode.startsWith("es", ignoreCase = true)) {
        nameEs.ifBlank {
            name
        }
    } else {
        name
    }

fun Product.localizedDescription(languageCode: String): String =
    if (languageCode.startsWith("es", ignoreCase = true)) {
        descriptionEs.ifBlank {
            description
        }
    } else {
        description
    }
