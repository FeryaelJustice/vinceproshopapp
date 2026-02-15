package com.billiardsdraw.vinceproshop.data.repository

import com.billiardsdraw.vinceproshop.core.DispatchersProvider
import com.billiardsdraw.vinceproshop.data.remote.CheckoutApi
import com.billiardsdraw.vinceproshop.data.remote.CreatePaymentIntentItemDto
import com.billiardsdraw.vinceproshop.data.remote.CreatePaymentIntentRequestDto
import com.billiardsdraw.vinceproshop.domain.model.CheckoutPaymentIntent
import com.billiardsdraw.vinceproshop.domain.model.CheckoutPaymentIntentPayload
import com.billiardsdraw.vinceproshop.domain.repository.CheckoutRepository
import kotlinx.coroutines.withContext

class CheckoutRepositoryImpl(
    private val api: CheckoutApi,
    private val dispatchers: DispatchersProvider,
) : CheckoutRepository {
    override suspend fun createPaymentIntent(payload: CheckoutPaymentIntentPayload): CheckoutPaymentIntent =
        withContext(dispatchers.io) {
            val response =
                api.createPaymentIntent(
                    CreatePaymentIntentRequestDto(
                        items =
                            payload.items.map {
                                CreatePaymentIntentItemDto(
                                    slug = it.slug,
                                    size = it.size,
                                    quantity = it.quantity,
                                )
                            },
                        currency = payload.currency,
                        customerEmail = payload.customerEmail,
                        customerName = payload.customerName,
                        address = payload.address,
                        country = payload.country,
                        phone = payload.phone,
                        locale = payload.locale,
                    ),
                )
            CheckoutPaymentIntent(
                clientSecret = response.clientSecret,
                amount = response.amount,
            )
        }
}
