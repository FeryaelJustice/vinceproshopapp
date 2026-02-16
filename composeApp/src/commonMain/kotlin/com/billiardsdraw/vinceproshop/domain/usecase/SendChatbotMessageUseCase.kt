package com.billiardsdraw.vinceproshop.domain.usecase

import com.billiardsdraw.vinceproshop.domain.model.ChatbotSendMessageResult
import com.billiardsdraw.vinceproshop.domain.model.ChatbotUploadAttachment
import com.billiardsdraw.vinceproshop.domain.repository.ChatbotRepository

class SendChatbotMessageUseCase(
    private val repository: ChatbotRepository,
) {
    suspend operator fun invoke(
        sessionId: String,
        clientToken: String,
        message: String,
        locale: String?,
        attachment: ChatbotUploadAttachment?,
    ): ChatbotSendMessageResult =
        repository.sendMessage(
            sessionId = sessionId,
            clientToken = clientToken,
            message = message,
            locale = locale,
            attachment = attachment,
        )
}
