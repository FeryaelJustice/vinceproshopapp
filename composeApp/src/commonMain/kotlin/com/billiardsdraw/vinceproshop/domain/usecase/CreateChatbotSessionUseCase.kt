package com.billiardsdraw.vinceproshop.domain.usecase

import com.billiardsdraw.vinceproshop.domain.model.ChatbotSession
import com.billiardsdraw.vinceproshop.domain.repository.ChatbotRepository

class CreateChatbotSessionUseCase(
    private val repository: ChatbotRepository,
) {
    suspend operator fun invoke(
        locale: String?,
        path: String?,
    ): ChatbotSession = repository.createSession(locale = locale, path = path)
}
