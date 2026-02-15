package com.billiardsdraw.vinceproshop.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class CreatePaymentIntentItemDto(
    val slug: String,
    val size: String,
    val quantity: Int,
)

@Serializable
data class CreatePaymentIntentRequestDto(
    val items: List<CreatePaymentIntentItemDto>,
    val currency: String,
    val customerEmail: String,
    val customerName: String,
    val address: String,
    val country: String,
    val phone: String,
    val locale: String? = null,
)

@Serializable
data class CreatePaymentIntentResponseDto(
    val clientSecret: String = "",
    @Serializable(with = LenientDoubleSerializer::class) val amount: Double = 0.0,
)

@Serializable
data class ApiErrorDto(
    val error: String? = null,
    val message: String? = null,
)
