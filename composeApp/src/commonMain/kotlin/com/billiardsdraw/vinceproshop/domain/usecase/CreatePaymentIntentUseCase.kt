package com.billiardsdraw.vinceproshop.domain.usecase

import com.billiardsdraw.vinceproshop.domain.model.CheckoutPaymentIntent
import com.billiardsdraw.vinceproshop.domain.model.CheckoutPaymentIntentPayload
import com.billiardsdraw.vinceproshop.domain.repository.CheckoutRepository

class CreatePaymentIntentUseCase(
    private val repository: CheckoutRepository,
) {
    suspend operator fun invoke(payload: CheckoutPaymentIntentPayload): CheckoutPaymentIntent {
        return repository.createPaymentIntent(payload)
    }
}
