package com.billiardsdraw.vinceproshop.data.remote

import com.billiardsdraw.vinceproshop.data.security.AuthTokenStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.Json

class KtorAccountApi(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val tokenStore: AuthTokenStore,
    private val json: Json,
) : AccountApi {

    override suspend fun login(identifier: String, password: String): LoginResponseDto {
        val requestPayload = json.encodeToString(
            serializer = LoginRequestDto.serializer(),
            value = LoginRequestDto(identifier = identifier, password = password),
        )
        val response = httpClient.post(url("auth/login")) {
            contentType(ContentType.Application.Json)
            setBody(requestPayload)
        }
        val payload = response.body<LoginResponseDto>()
        val tokenFromPayload = payload.token
            ?: payload.jwt
            ?: payload.accessToken
        val tokenFromCookie = extractTokenFromSetCookie(response.headers)
        val resolvedToken = tokenFromPayload
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: tokenFromCookie

        if (!resolvedToken.isNullOrBlank()) {
            tokenStore.saveToken(resolvedToken)
        }
        return payload
    }

    override suspend fun logout() {
        val token = tokenStore.readToken()
        runCatching {
            httpClient.post(url("auth/logout")) {
                applyAuthHeaders(token)
            }
        }
        tokenStore.clearToken()
    }

    override suspend fun me(): AuthSessionDto {
        val token = tokenStore.readToken() ?: return AuthSessionDto(isAuthenticated = false, user = null)
        return httpClient.get(url("auth/me")) {
            applyAuthHeaders(token)
        }.body()
    }

    override suspend fun userOrders(): List<OrderDto> {
        val token = tokenStore.readToken() ?: throw IllegalStateException("Authentication required")
        return httpClient.get(url("users/orders")) {
            applyAuthHeaders(token)
        }.body()
    }

    override suspend fun adminOrders(): List<OrderDto> {
        val token = tokenStore.readToken() ?: throw IllegalStateException("Authentication required")
        return httpClient.get(url("admin/orders")) {
            applyAuthHeaders(token)
        }.body()
    }

    private fun url(path: String): String = "${baseUrl.trimEnd('/')}/$path"

    private fun io.ktor.client.request.HttpRequestBuilder.applyAuthHeaders(token: String?) {
        val normalized = token?.trim().orEmpty()
        if (normalized.isBlank()) return
        header(HttpHeaders.Authorization, "Bearer $normalized")
        // /api/auth/me reads req.cookies.token directly in backend route.
        header(HttpHeaders.Cookie, "token=$normalized")
    }

    private fun extractTokenFromSetCookie(headers: Headers): String? {
        val setCookies = headers.getAll(HttpHeaders.SetCookie).orEmpty()
        for (cookieHeader in setCookies) {
            val cookiePair = cookieHeader.substringBefore(";").trim()
            if (cookiePair.startsWith("token=")) {
                return cookiePair.removePrefix("token=").trim().trim('"').takeIf { it.isNotBlank() }
            }
        }
        return null
    }
}
