package com.billiardsdraw.vinceproshop.data.remote

interface ChatbotApi {
    suspend fun createSession(
        locale: String?,
        path: String?,
    ): ChatbotSessionDto

    suspend fun sendMessage(
        sessionId: String,
        clientToken: String,
        message: String,
        locale: String?,
        attachment: ChatbotUploadAttachmentDto?,
    ): ChatbotSendMessageResponseDto

    suspend fun closeSession(
        sessionId: String,
        clientToken: String,
    )
}
