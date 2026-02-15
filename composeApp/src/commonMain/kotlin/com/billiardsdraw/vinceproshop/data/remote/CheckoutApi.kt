package com.billiardsdraw.vinceproshop.data.remote

interface CheckoutApi {
    suspend fun createPaymentIntent(request: CreatePaymentIntentRequestDto): CreatePaymentIntentResponseDto
}
