package com.billiardsdraw.vinceproshop.domain.repository

import com.billiardsdraw.vinceproshop.domain.model.ChatbotSendMessageResult
import com.billiardsdraw.vinceproshop.domain.model.ChatbotSession
import com.billiardsdraw.vinceproshop.domain.model.ChatbotUploadAttachment

interface ChatbotRepository {
    suspend fun createSession(
        locale: String?,
        path: String?,
    ): ChatbotSession

    suspend fun sendMessage(
        sessionId: String,
        clientToken: String,
        message: String,
        locale: String?,
        attachment: ChatbotUploadAttachment?,
    ): ChatbotSendMessageResult

    suspend fun closeSession(
        sessionId: String,
        clientToken: String,
    )
}
