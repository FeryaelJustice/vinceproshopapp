package com.billiardsdraw.vinceproshop.domain.model

data class ChatbotStarterTopic(
    val id: String,
    val label: String,
)

data class ChatbotReference(
    val type: String,
    val label: String,
    val href: String,
)

data class ChatbotAttachmentMetadata(
    val originalFilename: String,
    val sizeBytes: Int,
    val mimeType: String,
)

data class ChatbotMessageMetadata(
    val references: List<ChatbotReference> = emptyList(),
    val quickReplies: List<String> = emptyList(),
    val attachment: ChatbotAttachmentMetadata? = null,
)

enum class ChatbotSenderRole {
    User,
    Bot,
    System,
    Admin,
}

data class ChatbotMessage(
    val id: String,
    val senderRole: ChatbotSenderRole,
    val content: String,
    val contentLocale: String,
    val intent: String?,
    val createdAt: String,
    val metadata: ChatbotMessageMetadata? = null,
)

data class ChatbotSessionLimits(
    val maxMessageChars: Int,
    val maxAttachmentsPerChat: Int,
    val maxAttachmentSizeMb: Int,
)

data class ChatbotSession(
    val sessionId: String,
    val clientToken: String,
    val botName: String,
    val welcomeMessage: String,
    val starterTopics: List<ChatbotStarterTopic>,
    val limits: ChatbotSessionLimits,
)

data class ChatbotSendMessageResult(
    val userMessage: ChatbotMessage,
    val botMessage: ChatbotMessage,
    val attachmentCount: Int,
    val messageCount: Int,
)

data class ChatbotUploadAttachment(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChatbotUploadAttachment) return false
        if (fileName != other.fileName) return false
        if (mimeType != other.mimeType) return false
        if (!bytes.contentEquals(other.bytes)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}
