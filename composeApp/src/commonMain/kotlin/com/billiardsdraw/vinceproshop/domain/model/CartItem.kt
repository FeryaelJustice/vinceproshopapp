package com.billiardsdraw.vinceproshop.domain.model

data class CartItem(
    val slug: String,
    val size: String,
    val quantity: Int,
    val name: String,
    val nameEs: String,
    val imageUrl: String,
    val price: Double,
    val discount: Double,
    val originalPrice: Double,
)

val CartItem.finalPrice: Double
    get() = if (discount > 0.0) originalPrice * (1.0 - (discount / 100.0)) else price
