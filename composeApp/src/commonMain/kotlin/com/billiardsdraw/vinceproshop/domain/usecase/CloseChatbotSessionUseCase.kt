package com.billiardsdraw.vinceproshop.domain.usecase

import com.billiardsdraw.vinceproshop.domain.repository.ChatbotRepository

class CloseChatbotSessionUseCase(
    private val repository: ChatbotRepository,
) {
    suspend operator fun invoke(
        sessionId: String,
        clientToken: String,
    ) {
        repository.closeSession(sessionId = sessionId, clientToken = clientToken)
    }
}
