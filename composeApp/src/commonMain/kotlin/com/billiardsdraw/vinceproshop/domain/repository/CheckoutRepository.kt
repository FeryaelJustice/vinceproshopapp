package com.billiardsdraw.vinceproshop.domain.repository

import com.billiardsdraw.vinceproshop.domain.model.CheckoutPaymentIntent
import com.billiardsdraw.vinceproshop.domain.model.CheckoutPaymentIntentPayload

interface CheckoutRepository {
    suspend fun createPaymentIntent(payload: CheckoutPaymentIntentPayload): CheckoutPaymentIntent
}
