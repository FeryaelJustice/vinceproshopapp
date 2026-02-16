package com.billiardsdraw.vinceproshop.data.remote

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class ChatbotCreateSessionRequestDto(
    val locale: String? = null,
    val path: String? = null,
)

@Serializable
data class ChatbotStarterTopicDto(
    val id: String = "",
    val label: String = "",
)

@Serializable
data class ChatbotReferenceDto(
    val type: String = "",
    val label: String = "",
    val href: String = "",
)

@Serializable
data class ChatbotAttachmentMetadataDto(
    @SerialName("original_filename")
    val originalFilename: String = "",
    @SerialName("size_bytes")
    @Serializable(with = LenientIntSerializer::class)
    val sizeBytes: Int = 0,
    @SerialName("mime_type")
    val mimeType: String = "",
)

@Serializable
data class ChatbotMessageMetadataDto(
    val references: List<ChatbotReferenceDto>? = null,
    @SerialName("quick_replies")
    val quickReplies: List<String>? = null,
    val attachment: ChatbotAttachmentMetadataDto? = null,
)

@Serializable
data class ChatbotMessageDto(
    @Serializable(with = LenientMessageIdSerializer::class)
    val id: String = "",
    @SerialName("sender_role")
    val senderRole: String = "system",
    val content: String = "",
    @SerialName("content_locale")
    val contentLocale: String = "en",
    val intent: String? = null,
    @SerialName("created_at")
    val createdAt: String = "",
    val metadata: ChatbotMessageMetadataDto? = null,
)

@Serializable
data class ChatbotSessionLimitsDto(
    @SerialName("max_message_chars")
    @Serializable(with = LenientIntSerializer::class)
    val maxMessageChars: Int = 1000,
    @SerialName("max_attachments_per_chat")
    @Serializable(with = LenientIntSerializer::class)
    val maxAttachmentsPerChat: Int = 6,
    @SerialName("max_attachment_size_mb")
    @Serializable(with = LenientIntSerializer::class)
    val maxAttachmentSizeMb: Int = 10,
)

@Serializable
data class ChatbotSessionDto(
    @SerialName("session_id")
    val sessionId: String = "",
    @SerialName("client_token")
    val clientToken: String = "",
    @SerialName("bot_name")
    val botName: String = "",
    @SerialName("welcome_message")
    val welcomeMessage: String = "",
    @SerialName("starter_topics")
    val starterTopics: List<ChatbotStarterTopicDto> = emptyList(),
    val limits: ChatbotSessionLimitsDto = ChatbotSessionLimitsDto(),
)

@Serializable
data class ChatbotSendMessageResponseDto(
    @SerialName("user_message")
    val userMessage: ChatbotMessageDto = ChatbotMessageDto(),
    @SerialName("bot_message")
    val botMessage: ChatbotMessageDto = ChatbotMessageDto(),
    @SerialName("attachment_count")
    @Serializable(with = LenientIntSerializer::class)
    val attachmentCount: Int = 0,
    @SerialName("message_count")
    @Serializable(with = LenientIntSerializer::class)
    val messageCount: Int = 0,
)

data class ChatbotUploadAttachmentDto(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChatbotUploadAttachmentDto) return false
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

object LenientMessageIdSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LenientMessageId", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeString()
        val element = jsonDecoder.decodeJsonElement()
        return parseMessageId(element)
    }

    override fun serialize(
        encoder: Encoder,
        value: String,
    ) {
        encoder.encodeString(value)
    }
}

private fun parseMessageId(element: JsonElement): String {
    if (element is JsonNull) return ""
    val primitive = element as? JsonPrimitive ?: return element.toString()
    return primitive.content
}
