package com.billiardsdraw.vinceproshop.data.repository

import com.billiardsdraw.vinceproshop.core.DispatchersProvider
import com.billiardsdraw.vinceproshop.data.remote.ChatbotApi
import com.billiardsdraw.vinceproshop.data.remote.ChatbotAttachmentMetadataDto
import com.billiardsdraw.vinceproshop.data.remote.ChatbotMessageDto
import com.billiardsdraw.vinceproshop.data.remote.ChatbotMessageMetadataDto
import com.billiardsdraw.vinceproshop.data.remote.ChatbotReferenceDto
import com.billiardsdraw.vinceproshop.data.remote.ChatbotSessionDto
import com.billiardsdraw.vinceproshop.data.remote.ChatbotSendMessageResponseDto
import com.billiardsdraw.vinceproshop.data.remote.ChatbotStarterTopicDto
import com.billiardsdraw.vinceproshop.data.remote.ChatbotUploadAttachmentDto
import com.billiardsdraw.vinceproshop.domain.model.ChatbotAttachmentMetadata
import com.billiardsdraw.vinceproshop.domain.model.ChatbotMessage
import com.billiardsdraw.vinceproshop.domain.model.ChatbotMessageMetadata
import com.billiardsdraw.vinceproshop.domain.model.ChatbotReference
import com.billiardsdraw.vinceproshop.domain.model.ChatbotSendMessageResult
import com.billiardsdraw.vinceproshop.domain.model.ChatbotSenderRole
import com.billiardsdraw.vinceproshop.domain.model.ChatbotSession
import com.billiardsdraw.vinceproshop.domain.model.ChatbotSessionLimits
import com.billiardsdraw.vinceproshop.domain.model.ChatbotStarterTopic
import com.billiardsdraw.vinceproshop.domain.model.ChatbotUploadAttachment
import com.billiardsdraw.vinceproshop.domain.repository.ChatbotRepository
import kotlinx.coroutines.withContext

class ChatbotRepositoryImpl(
    private val api: ChatbotApi,
    private val dispatchers: DispatchersProvider,
) : ChatbotRepository {
    override suspend fun createSession(
        locale: String?,
        path: String?,
    ): ChatbotSession =
        withContext(dispatchers.io) {
            api.createSession(locale = locale, path = path).toDomain()
        }

    override suspend fun sendMessage(
        sessionId: String,
        clientToken: String,
        message: String,
        locale: String?,
        attachment: ChatbotUploadAttachment?,
    ): ChatbotSendMessageResult =
        withContext(dispatchers.io) {
            api.sendMessage(
                sessionId = sessionId,
                clientToken = clientToken,
                message = message,
                locale = locale,
                attachment = attachment?.toDto(),
            ).toDomain()
        }

    override suspend fun closeSession(
        sessionId: String,
        clientToken: String,
    ) {
        withContext(dispatchers.io) {
            api.closeSession(sessionId = sessionId, clientToken = clientToken)
        }
    }
}

private fun ChatbotSessionDto.toDomain(): ChatbotSession =
    ChatbotSession(
        sessionId = sessionId,
        clientToken = clientToken,
        botName = botName,
        welcomeMessage = welcomeMessage,
        starterTopics = starterTopics.map(ChatbotStarterTopicDto::toDomain),
        limits =
            ChatbotSessionLimits(
                maxMessageChars = limits.maxMessageChars.coerceAtLeast(1),
                maxAttachmentsPerChat = limits.maxAttachmentsPerChat.coerceAtLeast(0),
                maxAttachmentSizeMb = limits.maxAttachmentSizeMb.coerceAtLeast(1),
            ),
    )

private fun ChatbotSendMessageResponseDto.toDomain(): ChatbotSendMessageResult =
    ChatbotSendMessageResult(
        userMessage = userMessage.toDomain(),
        botMessage = botMessage.toDomain(),
        attachmentCount = attachmentCount.coerceAtLeast(0),
        messageCount = messageCount.coerceAtLeast(0),
    )

private fun ChatbotMessageDto.toDomain(): ChatbotMessage =
    ChatbotMessage(
        id = id,
        senderRole = senderRole.toDomainSenderRole(),
        content = content,
        contentLocale = contentLocale,
        intent = intent,
        createdAt = createdAt,
        metadata = metadata?.toDomain(),
    )

private fun ChatbotMessageMetadataDto.toDomain(): ChatbotMessageMetadata =
    ChatbotMessageMetadata(
        references = references.orEmpty().map(ChatbotReferenceDto::toDomain),
        quickReplies = quickReplies.orEmpty(),
        attachment = attachment?.toDomain(),
    )

private fun ChatbotReferenceDto.toDomain(): ChatbotReference =
    ChatbotReference(
        type = type,
        label = label,
        href = href,
    )

private fun ChatbotAttachmentMetadataDto.toDomain(): ChatbotAttachmentMetadata =
    ChatbotAttachmentMetadata(
        originalFilename = originalFilename,
        sizeBytes = sizeBytes.coerceAtLeast(0),
        mimeType = mimeType,
    )

private fun ChatbotStarterTopicDto.toDomain(): ChatbotStarterTopic =
    ChatbotStarterTopic(
        id = id,
        label = label,
    )

private fun String.toDomainSenderRole(): ChatbotSenderRole =
    when (trim().lowercase()) {
        "user" -> ChatbotSenderRole.User
        "bot" -> ChatbotSenderRole.Bot
        "admin" -> ChatbotSenderRole.Admin
        else -> ChatbotSenderRole.System
    }

private fun ChatbotUploadAttachment.toDto(): ChatbotUploadAttachmentDto =
    ChatbotUploadAttachmentDto(
        fileName = fileName,
        mimeType = mimeType,
        bytes = bytes,
    )
