package com.billiardsdraw.vinceproshop.data.remote

import com.billiardsdraw.vinceproshop.data.security.AuthTokenStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class KtorCheckoutApi(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val tokenStore: AuthTokenStore,
) : CheckoutApi {

    override suspend fun createPaymentIntent(request: CreatePaymentIntentRequestDto): CreatePaymentIntentResponseDto {
        val token = tokenStore.readToken()
        val response = httpClient.post(url()) {
            applyAuthHeaders(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        if (!response.status.isSuccess()) {
            val apiError = runCatching { response.body<ApiErrorDto>() }.getOrNull()
            val message = apiError?.error
                ?: apiError?.message
                ?: "Failed to create payment intent (${response.status.value})"
            throw IllegalStateException(message)
        }

        return response.body()
    }

    private fun url(): String = "${baseUrl.trimEnd('/')}/payments/create-intent"

    private fun io.ktor.client.request.HttpRequestBuilder.applyAuthHeaders(token: String?) {
        val normalized = token?.trim().orEmpty()
        if (normalized.isBlank()) return
        header(HttpHeaders.Authorization, "Bearer $normalized")
        header(HttpHeaders.Cookie, "token=$normalized")
    }
}
