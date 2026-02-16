package com.billiardsdraw.vinceproshop.presentation.chatbot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billiardsdraw.vinceproshop.core.DispatchersProvider
import com.billiardsdraw.vinceproshop.domain.model.ChatbotAttachmentMetadata
import com.billiardsdraw.vinceproshop.domain.model.ChatbotMessage
import com.billiardsdraw.vinceproshop.domain.model.ChatbotMessageMetadata
import com.billiardsdraw.vinceproshop.domain.model.ChatbotSendMessageResult
import com.billiardsdraw.vinceproshop.domain.model.ChatbotSenderRole
import com.billiardsdraw.vinceproshop.domain.model.ChatbotSession
import com.billiardsdraw.vinceproshop.domain.model.ChatbotUploadAttachment
import com.billiardsdraw.vinceproshop.domain.usecase.CloseChatbotSessionUseCase
import com.billiardsdraw.vinceproshop.domain.usecase.CreateChatbotSessionUseCase
import com.billiardsdraw.vinceproshop.domain.usecase.SendChatbotMessageUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatbotUiAttachment(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChatbotUiAttachment) return false
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

data class ChatbotUiMessage(
    val localId: String,
    val senderRole: ChatbotSenderRole,
    val content: String,
    val contentLocale: String,
    val intent: String?,
    val createdAt: String,
    val metadata: ChatbotMessageMetadata? = null,
)

data class ChatbotUiState(
    val isOpen: Boolean = false,
    val locale: String = "en",
    val session: ChatbotSession? = null,
    val messages: List<ChatbotUiMessage> = emptyList(),
    val inputText: String = "",
    val selectedAttachment: ChatbotUiAttachment? = null,
    val attachmentCount: Int = 0,
    val isBootstrapping: Boolean = false,
    val isSending: Boolean = false,
    val isBotTyping: Boolean = false,
    val errorMessage: String? = null,
)

class ChatbotViewModel(
    private val createChatbotSession: CreateChatbotSessionUseCase,
    private val sendChatbotMessage: SendChatbotMessageUseCase,
    private val closeChatbotSession: CloseChatbotSessionUseCase,
    private val dispatchers: DispatchersProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(ChatbotUiState())
    val state: StateFlow<ChatbotUiState> = _state.asStateFlow()

    private var bootstrapJob: Job? = null
    private var sendJob: Job? = null
    private var localMessageCounter: Long = 1L

    fun open(
        locale: String,
        path: String? = null,
    ) {
        val normalizedLocale = normalizeLocale(locale)
        _state.value =
            _state.value.copy(
                isOpen = true,
                locale = normalizedLocale,
                errorMessage = null,
            )
        bootstrapIfNeeded(path = path)
    }

    fun hide() {
        if (!_state.value.isOpen) return
        _state.value = _state.value.copy(isOpen = false)
    }

    fun close() {
        val session = _state.value.session
        bootstrapJob?.cancel()
        sendJob?.cancel()
        _state.value = ChatbotUiState()

        if (session != null) {
            viewModelScope.launch(dispatchers.io) {
                runCatching {
                    closeChatbotSession(
                        sessionId = session.sessionId,
                        clientToken = session.clientToken,
                    )
                }
            }
        }
    }

    fun onInputChanged(value: String) {
        _state.value = _state.value.copy(inputText = value, errorMessage = null)
    }

    fun setError(message: String?) {
        val normalized = message?.trim().orEmpty()
        if (normalized.isBlank()) return
        _state.value = _state.value.copy(errorMessage = normalized)
    }

    fun onAttachmentPicked(attachment: ChatbotUiAttachment) {
        val state = _state.value
        val limits = state.session?.limits
        val maxAttachments = limits?.maxAttachmentsPerChat ?: 6
        val maxBytes = ((limits?.maxAttachmentSizeMb ?: 10).coerceAtLeast(1)) * 1024 * 1024

        if (state.attachmentCount >= maxAttachments) {
            _state.value =
                state.copy(
                    errorMessage = localizedError(state.locale, "Attachment limit reached.", "Has superado el limite de adjuntos."),
                )
            return
        }
        if (attachment.bytes.isEmpty()) {
            _state.value =
                state.copy(
                    errorMessage = localizedError(state.locale, "Selected file is empty.", "El archivo seleccionado esta vacio."),
                )
            return
        }
        if (attachment.bytes.size > maxBytes) {
            val sizeMb = maxBytes / (1024 * 1024)
            _state.value =
                state.copy(
                    errorMessage =
                        localizedError(
                            state.locale,
                            "File exceeds $sizeMb MB.",
                            "El archivo supera los $sizeMb MB.",
                        ),
                )
            return
        }

        _state.value =
            state.copy(
                selectedAttachment = attachment,
                errorMessage = null,
            )
    }

    fun clearAttachment() {
        _state.value = _state.value.copy(selectedAttachment = null, errorMessage = null)
    }

    fun sendMessage(rawMessage: String? = null) {
        val snapshot = _state.value
        val session = snapshot.session ?: return
        if (snapshot.isSending || snapshot.isBootstrapping) return

        val message = (rawMessage ?: snapshot.inputText).trim()
        val maxMessageChars = session.limits.maxMessageChars
        if (message.isBlank()) {
            _state.value =
                snapshot.copy(
                    errorMessage = localizedError(snapshot.locale, "Message is required.", "Debes escribir un mensaje."),
                )
            return
        }
        if (message.length > maxMessageChars) {
            _state.value =
                snapshot.copy(
                    errorMessage =
                        localizedError(
                            snapshot.locale,
                            "Message exceeds $maxMessageChars characters.",
                            "El mensaje supera $maxMessageChars caracteres.",
                        ),
                )
            return
        }
        if (snapshot.selectedAttachment != null && snapshot.attachmentCount >= session.limits.maxAttachmentsPerChat) {
            _state.value =
                snapshot.copy(
                    errorMessage =
                        localizedError(
                            snapshot.locale,
                            "Attachment limit reached for this chat.",
                            "Has alcanzado el limite de adjuntos para este chat.",
                        ),
                )
            return
        }

        val attachmentForRequest = snapshot.selectedAttachment
        val optimistic =
            ChatbotUiMessage(
                localId = nextLocalId(prefix = "user"),
                senderRole = ChatbotSenderRole.User,
                content = message,
                contentLocale = snapshot.locale,
                intent = null,
                createdAt = "now",
                metadata =
                    attachmentForRequest?.let {
                        ChatbotMessageMetadata(
                            attachment =
                                ChatbotAttachmentMetadata(
                                    originalFilename = it.fileName,
                                    sizeBytes = it.bytes.size,
                                    mimeType = it.mimeType,
                                ),
                        )
                    },
            )

        _state.value =
            snapshot.copy(
                messages = snapshot.messages + optimistic,
                inputText = "",
                selectedAttachment = null,
                isSending = true,
                isBotTyping = false,
                errorMessage = null,
            )

        val sessionId = session.sessionId
        val clientToken = session.clientToken
        val locale = snapshot.locale

        sendJob?.cancel()
        sendJob =
            viewModelScope.launch(dispatchers.io) {
                runCatching {
                    sendChatbotMessage(
                        sessionId = sessionId,
                        clientToken = clientToken,
                        message = message,
                        locale = locale,
                        attachment =
                            attachmentForRequest?.let {
                                ChatbotUploadAttachment(
                                    fileName = it.fileName,
                                    mimeType = it.mimeType,
                                    bytes = it.bytes,
                                )
                            },
                    )
                }.onSuccess { response ->
                    handleSendSuccess(sessionId = sessionId, response = response)
                }.onFailure { error ->
                    if (!_state.value.isOpen) return@onFailure
                    _state.value =
                        _state.value.copy(
                            isSending = false,
                            isBotTyping = false,
                            errorMessage =
                                error.message
                                    ?: localizedError(locale, "Could not send message.", "No se pudo enviar el mensaje."),
                        )
                }
            }
    }

    private fun bootstrapIfNeeded(path: String?) {
        val snapshot = _state.value
        if (!snapshot.isOpen || snapshot.session != null || snapshot.isBootstrapping) return

        bootstrapJob?.cancel()
        bootstrapJob =
            viewModelScope.launch(dispatchers.io) {
                _state.value = _state.value.copy(isBootstrapping = true, errorMessage = null)
                val locale = _state.value.locale

                runCatching {
                    createChatbotSession(
                        locale = locale,
                        path = path?.trim()?.ifBlank { "/" } ?: "/",
                    )
                }.onSuccess { session ->
                    if (!_state.value.isOpen) {
                        runCatching { closeChatbotSession(session.sessionId, session.clientToken) }
                        return@onSuccess
                    }
                    val welcome =
                        ChatbotUiMessage(
                            localId = nextLocalId(prefix = "bot"),
                            senderRole = ChatbotSenderRole.Bot,
                            content = session.welcomeMessage,
                            contentLocale = locale,
                            intent = "welcome",
                            createdAt = "now",
                            metadata =
                                ChatbotMessageMetadata(
                                    quickReplies = session.starterTopics.map { it.label },
                                ),
                        )
                    _state.value =
                        _state.value.copy(
                            session = session,
                            messages = listOf(welcome),
                            inputText = "",
                            selectedAttachment = null,
                            attachmentCount = 0,
                            isBootstrapping = false,
                            isSending = false,
                            isBotTyping = false,
                            errorMessage = null,
                        )
                }.onFailure { error ->
                    if (!_state.value.isOpen) return@onFailure
                    _state.value =
                        _state.value.copy(
                            isBootstrapping = false,
                            errorMessage =
                                error.message
                                    ?: localizedError(locale, "Could not start chat.", "No se pudo iniciar el chat."),
                        )
                }
            }
    }

    private suspend fun handleSendSuccess(
        sessionId: String,
        response: ChatbotSendMessageResult,
    ) {
        if (!_state.value.isOpen || _state.value.session?.sessionId != sessionId) return

        _state.value =
            _state.value.copy(
                attachmentCount = response.attachmentCount,
                isBotTyping = true,
                errorMessage = null,
            )

        delay(simulatedTypingDelayMillis(response.botMessage.content))

        if (!_state.value.isOpen || _state.value.session?.sessionId != sessionId) return

        _state.value =
            _state.value.copy(
                messages = _state.value.messages + response.botMessage.toUiMessage(localId = nextLocalId(prefix = "bot")),
                isSending = false,
                isBotTyping = false,
            )
    }

    private fun ChatbotMessage.toUiMessage(localId: String): ChatbotUiMessage =
        ChatbotUiMessage(
            localId = localId,
            senderRole = senderRole,
            content = content,
            contentLocale = contentLocale,
            intent = intent,
            createdAt = createdAt,
            metadata = metadata,
        )

    private fun simulatedTypingDelayMillis(content: String): Long {
        val scaled = content.length * 4
        return scaled.coerceIn(minimumValue = 450, maximumValue = 1200).toLong()
    }

    private fun nextLocalId(prefix: String): String {
        val id = "$prefix-$localMessageCounter"
        localMessageCounter += 1
        return id
    }

    private fun normalizeLocale(raw: String): String =
        if (raw.startsWith("es", ignoreCase = true)) "es" else "en"

    private fun localizedError(
        locale: String,
        en: String,
        es: String,
    ): String = if (locale.startsWith("es", ignoreCase = true)) es else en
}
