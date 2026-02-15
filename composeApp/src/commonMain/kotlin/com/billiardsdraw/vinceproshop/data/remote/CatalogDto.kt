package com.billiardsdraw.vinceproshop.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CategoryDto(
    val id: String,
    val name: String = "",
    @SerialName("name_es") val nameEs: String = "",
    val image: String = "",
    @SerialName("parent_id") val parentId: String? = null,
    @SerialName("is_cue") val isCue: Int? = null,
)

@Serializable
data class ProductOptionDto(
    val label: String = "",
    val value: String = "unique",
    val quantity: Int? = null,
    val price: Double? = null,
    val discount: Double? = null,
)

@Serializable
data class ProductMediaDto(
    val url: String = "",
)

@Serializable
data class ProductDto(
    val id: Int,
    val slug: String,
    val name: String = "",
    @SerialName("name_es") val nameEs: String = "",
    val vendor: String = "",
    val price: Double = 0.0,
    @SerialName("category_id") val categoryId: String = "",
    val description: String = "",
    @SerialName("description_es") val descriptionEs: String = "",
    val image: String? = null,
    @SerialName("inStock") val inStock: Int? = null,
    @SerialName("max_discount") val maxDiscount: Double? = null,
    val media: List<ProductMediaDto>? = null,
    val options: List<ProductOptionDto>? = null,
)

@Serializable
data class FeaturedSlideDto(
    val id: Int,
    @SerialName("target_type") val targetType: String = "product",
    @SerialName("product_id") val productId: Int? = null,
    @SerialName("category_id") val categoryId: String? = null,
    val slug: String? = null,
    val name: String = "",
    @SerialName("name_es") val nameEs: String = "",
    val image: String = "",
    val title: String = "",
    @SerialName("title_es") val titleEs: String = "",
    val subtitle: String = "",
    @SerialName("subtitle_es") val subtitleEs: String = "",
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("is_active") val isActive: Int = 0,
)
