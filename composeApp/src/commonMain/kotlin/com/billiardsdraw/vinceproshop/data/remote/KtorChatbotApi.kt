package com.billiardsdraw.vinceproshop.data.remote

import com.billiardsdraw.vinceproshop.data.security.AuthTokenStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess

class KtorChatbotApi(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val tokenStore: AuthTokenStore,
) : ChatbotApi {
    override suspend fun createSession(
        locale: String?,
        path: String?,
    ): ChatbotSessionDto {
        val token = tokenStore.readToken()
        val response =
            httpClient.post(url("chatbot/sessions")) {
                applyAuthHeaders(token)
                contentType(ContentType.Application.Json)
                setBody(
                    ChatbotCreateSessionRequestDto(
                        locale = locale?.trim()?.ifBlank { null },
                        path = path?.trim()?.ifBlank { null },
                    ),
                )
            }

        if (!response.status.isSuccess()) {
            throw IllegalStateException(
                errorMessage(
                    fallback = "Failed to create chatbot session (${response.status.value})",
                    extract = { response.body<ApiErrorDto>() },
                ),
            )
        }

        return response.body()
    }

    override suspend fun sendMessage(
        sessionId: String,
        clientToken: String,
        message: String,
        locale: String?,
        attachment: ChatbotUploadAttachmentDto?,
    ): ChatbotSendMessageResponseDto {
        val token = tokenStore.readToken()
        val encodedSessionId = sessionId.encodeURLPathPart()
        val body =
            MultiPartFormDataContent(
                formData {
                    append("message", message)
                    locale?.trim()?.takeIf { it.isNotBlank() }?.let { append("locale", it) }
                    attachment?.let { file ->
                        append(
                            key = "attachment",
                            value = file.bytes,
                            headers = attachmentPartHeaders(file),
                        )
                    }
                },
            )

        val response =
            httpClient.post(url("chatbot/sessions/$encodedSessionId/messages")) {
                applyAuthHeaders(token)
                header("X-Chat-Token", clientToken.trim())
                setBody(body)
            }

        if (!response.status.isSuccess()) {
            throw IllegalStateException(
                errorMessage(
                    fallback = "Failed to send chatbot message (${response.status.value})",
                    extract = { response.body<ApiErrorDto>() },
                ),
            )
        }

        return response.body()
    }

    override suspend fun closeSession(
        sessionId: String,
        clientToken: String,
    ) {
        val token = tokenStore.readToken()
        val encodedSessionId = sessionId.encodeURLPathPart()
        val response =
            httpClient.post(url("chatbot/sessions/$encodedSessionId/close")) {
                applyAuthHeaders(token)
                header("X-Chat-Token", clientToken.trim())
            }

        if (!response.status.isSuccess()) {
            throw IllegalStateException(
                errorMessage(
                    fallback = "Failed to close chatbot session (${response.status.value})",
                    extract = { response.body<ApiErrorDto>() },
                ),
            )
        }
    }

    private fun url(path: String): String = "${baseUrl.trimEnd('/')}/$path"

    private fun HttpRequestBuilder.applyAuthHeaders(token: String?) {
        val normalized = token?.trim().orEmpty()
        if (normalized.isBlank()) return
        header(HttpHeaders.Authorization, "Bearer $normalized")
        header(HttpHeaders.Cookie, "token=$normalized")
    }

    private fun attachmentPartHeaders(file: ChatbotUploadAttachmentDto): Headers =
        Headers.build {
            append(
                HttpHeaders.ContentType,
                file.mimeType.trim().ifBlank { ContentType.Application.OctetStream.toString() },
            )
            append(
                HttpHeaders.ContentDisposition,
                ContentDisposition.File
                    .withParameter(ContentDisposition.Parameters.Name, "attachment")
                    .withParameter(ContentDisposition.Parameters.FileName, file.fileName)
                    .toString(),
            )
        }

    private inline fun errorMessage(
        fallback: String,
        extract: () -> ApiErrorDto,
    ): String {
        val payload = runCatching { extract() }.getOrNull()
        return payload?.message?.takeIf { it.isNotBlank() }
            ?: payload?.error?.takeIf { it.isNotBlank() }
            ?: fallback
    }
}
