package com.billiardsdraw.vinceproshop.data.mapper

import com.billiardsdraw.vinceproshop.data.local.CartItemEntity
import com.billiardsdraw.vinceproshop.data.local.CategoryEntity
import com.billiardsdraw.vinceproshop.data.local.FeaturedSlideEntity
import com.billiardsdraw.vinceproshop.data.local.ProductEntity
import com.billiardsdraw.vinceproshop.data.remote.CategoryDto
import com.billiardsdraw.vinceproshop.data.remote.FeaturedSlideDto
import com.billiardsdraw.vinceproshop.data.remote.ProductDto
import com.billiardsdraw.vinceproshop.data.remote.ProductMediaDto
import com.billiardsdraw.vinceproshop.data.remote.ProductOptionDto
import com.billiardsdraw.vinceproshop.domain.model.CartItem
import com.billiardsdraw.vinceproshop.domain.model.Category
import com.billiardsdraw.vinceproshop.domain.model.FeaturedSlide
import com.billiardsdraw.vinceproshop.domain.model.Product
import com.billiardsdraw.vinceproshop.domain.model.ProductMedia
import com.billiardsdraw.vinceproshop.domain.model.ProductOption
import kotlinx.serialization.json.Json

fun ProductDto.toDomain(apiBaseUrl: String): Product {
    val resolvedImage = resolveAssetUrl(image.orEmpty(), apiBaseUrl)
    val mediaList = (media ?: emptyList()).map { it.toDomain(apiBaseUrl) }
    return Product(
        id = id,
        slug = slug,
        name = name,
        nameEs = nameEs,
        vendor = vendor,
        price = price,
        categoryId = categoryId,
        description = description,
        descriptionEs = descriptionEs,
        imageUrl = resolvedImage.ifBlank { mediaList.firstOrNull()?.url.orEmpty() },
        inStock = inStock ?: options?.sumOf { it.quantity ?: 0 } ?: 0,
        maxDiscount = maxDiscount ?: options?.maxOfOrNull { it.discount ?: 0.0 } ?: 0.0,
        media = mediaList,
        options = (options ?: emptyList()).map { it.toDomain() },
    )
}

fun ProductOptionDto.toDomain(): ProductOption {
    return ProductOption(
        label = label,
        value = value,
        quantity = quantity ?: 0,
        price = price ?: 0.0,
        discount = discount ?: 0.0,
    )
}

fun ProductMediaDto.toDomain(apiBaseUrl: String): ProductMedia {
    return ProductMedia(url = resolveAssetUrl(url, apiBaseUrl))
}

fun CategoryDto.toDomain(apiBaseUrl: String): Category {
    return Category(
        id = id,
        name = name,
        nameEs = nameEs,
        imageUrl = resolveAssetUrl(image, apiBaseUrl),
        parentId = parentId,
        isCue = (isCue ?: 0) == 1,
    )
}

fun FeaturedSlideDto.toDomain(apiBaseUrl: String): FeaturedSlide {
    return FeaturedSlide(
        id = id,
        targetType = targetType,
        productId = productId,
        categoryId = categoryId,
        slug = slug,
        name = name,
        nameEs = nameEs,
        imageUrl = resolveAssetUrl(image, apiBaseUrl),
        title = title,
        titleEs = titleEs,
        subtitle = subtitle,
        subtitleEs = subtitleEs,
        sortOrder = sortOrder,
        isActive = isActive == 1,
    )
}

fun Product.toEntity(json: Json): ProductEntity {
    return ProductEntity(
        slug = slug,
        payloadJson = json.encodeToString(Product.serializer(), this),
        name = name,
        nameEs = nameEs,
        vendor = vendor,
        categoryId = categoryId,
        price = price,
        imageUrl = imageUrl,
        inStock = inStock,
        updatedAtEpochMs = 0L,
    )
}

fun ProductEntity.toDomain(json: Json): Product {
    return runCatching {
        json.decodeFromString(Product.serializer(), payloadJson)
    }.getOrElse {
        Product(
            id = 0,
            slug = slug,
            name = name,
            nameEs = nameEs,
            vendor = vendor,
            price = price,
            categoryId = categoryId,
            description = "",
            descriptionEs = "",
            imageUrl = imageUrl,
            inStock = inStock,
            maxDiscount = 0.0,
            media = if (imageUrl.isBlank()) emptyList() else listOf(ProductMedia(imageUrl)),
            options = emptyList(),
        )
    }
}

fun Category.toEntity(): CategoryEntity {
    return CategoryEntity(
        id = id,
        name = name,
        nameEs = nameEs,
        imageUrl = imageUrl,
        parentId = parentId,
        isCue = isCue,
    )
}

fun CategoryEntity.toDomain(): Category {
    return Category(
        id = id,
        name = name,
        nameEs = nameEs,
        imageUrl = imageUrl,
        parentId = parentId,
        isCue = isCue,
    )
}

fun FeaturedSlide.toEntity(json: Json): FeaturedSlideEntity {
    return FeaturedSlideEntity(
        id = id,
        payloadJson = json.encodeToString(FeaturedSlide.serializer(), this),
        sortOrder = sortOrder,
        isActive = isActive,
    )
}

fun FeaturedSlideEntity.toDomain(json: Json): FeaturedSlide {
    return json.decodeFromString(FeaturedSlide.serializer(), payloadJson)
}

fun CartItemEntity.toDomain(): CartItem {
    return CartItem(
        slug = slug,
        size = size,
        quantity = quantity,
        name = name,
        nameEs = nameEs,
        imageUrl = imageUrl,
        price = price,
        discount = discount,
        originalPrice = originalPrice,
    )
}

fun CartItem.toEntity(): CartItemEntity {
    return CartItemEntity(
        slug = slug,
        size = size,
        quantity = quantity,
        name = name,
        nameEs = nameEs,
        imageUrl = imageUrl,
        price = price,
        discount = discount,
        originalPrice = originalPrice,
    )
}

private fun resolveAssetUrl(raw: String, apiBaseUrl: String): String {
    if (raw.isBlank()) return raw
    if (!raw.startsWith("/")) return raw
    val root = apiBaseUrl.substringBefore("/api").trimEnd('/')
    return "$root$raw"
}
