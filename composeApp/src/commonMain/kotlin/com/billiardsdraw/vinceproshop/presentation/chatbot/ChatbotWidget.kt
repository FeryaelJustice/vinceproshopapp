package com.billiardsdraw.vinceproshop.presentation.chatbot

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.billiardsdraw.vinceproshop.domain.model.ChatbotSenderRole
import com.billiardsdraw.vinceproshop.presentation.admin.detectImageMimeType
import io.github.ismoy.imagepickerkmp.domain.extensions.loadBytes
import io.github.ismoy.imagepickerkmp.domain.models.GalleryPhotoResult
import io.github.ismoy.imagepickerkmp.domain.models.MimeType
import io.github.ismoy.imagepickerkmp.presentation.ui.components.GalleryPickerLauncher
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatbotFloatingWidget(
    languageCode: String,
    currentPath: String,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: ChatbotViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val session = state.session
    val isSpanish = languageCode.startsWith("es", ignoreCase = true)
    val uriHandler = LocalUriHandler.current
    val listState = rememberLazyListState()
    val widgetScrollState = rememberScrollState()
    val overlayInteraction = remember { MutableInteractionSource() }
    val cardInteraction = remember { MutableInteractionSource() }
    var openAttachmentPicker by remember { mutableStateOf(false) }

    val maxMessageChars = session?.limits?.maxMessageChars ?: 1000
    val maxAttachments = session?.limits?.maxAttachmentsPerChat ?: 6
    val charCount = state.inputText.length
    val charLimitReached = charCount > maxMessageChars
    val hasUserMessages = state.messages.any { it.senderRole == ChatbotSenderRole.User }
    val latestBotMessage = state.messages.lastOrNull { it.senderRole == ChatbotSenderRole.Bot }
    val canUploadAttachment = state.attachmentCount < maxAttachments

    if (openAttachmentPicker) {
        GalleryPickerLauncher(
            onPhotosSelected = { photos ->
                val selected = photos.firstOrNull()
                if (selected != null) {
                    val bytes = selected.loadBytes()
                    val mimeType = detectAttachmentMimeType(selected, bytes)
                    val fileName =
                        selected.fileName
                            ?.takeIf { it.isNotBlank() }
                            ?: "attachment.${fileExtensionForAttachmentMimeType(mimeType)}"
                    viewModel.onAttachmentPicked(
                        ChatbotUiAttachment(
                            fileName = fileName,
                            mimeType = mimeType,
                            bytes = bytes,
                        ),
                    )
                }
                openAttachmentPicker = false
            },
            onError = {
                viewModel.setError(
                    it.message?.takeIf { msg -> msg.isNotBlank() }
                        ?: if (isSpanish) "No se pudo abrir el selector de archivos." else "Could not open file picker.",
                )
                openAttachmentPicker = false
            },
            onDismiss = { openAttachmentPicker = false },
            allowMultiple = false,
            mimeTypes =
                listOf(
                    MimeType.IMAGE_JPEG,
                    MimeType.IMAGE_PNG,
                    MimeType.IMAGE_WEBP,
                    MimeType.APPLICATION_PDF,
                ),
            selectionLimit = 1L,
        )
    }

    LaunchedEffect(state.messages.size, state.isBotTyping, state.isOpen) {
        if (!state.isOpen) return@LaunchedEffect
        val totalItems = state.messages.size + if (state.isBotTyping) 1 else 0
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.ime),
    ) {
        val anchoredModifier =
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp)

        if (!state.isOpen) {
            PlatformChatbotFloatingActionButton(
                onClick = { viewModel.open(locale = languageCode, path = currentPath) },
                modifier = anchoredModifier,
            )
        } else {
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = overlayInteraction,
                            indication = null,
                            onClick = viewModel::hide,
                        ),
            )

            Card(
                modifier =
                    anchoredModifier
                        .widthIn(min = 280.dp, max = 430.dp)
                        .fillMaxWidth(0.94f)
                        .clickable(
                            interactionSource = cardInteraction,
                            indication = null,
                            onClick = {},
                        ),
                shape = RoundedCornerShape(18.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(widgetScrollState),
                ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BotBadge()
                        Column {
                            Text(
                                text = session?.botName?.ifBlank { null }
                                    ?: if (isSpanish) "Vince Pro Shop IA Bot" else "Vince Pro Shop AI Bot",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = if (isSpanish) "Asistente inteligente de tienda" else "Smart store assistant",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    IconButton(onClick = viewModel::close) {
                        ChatbotCloseIcon(
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp, max = 360.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    state.errorMessage?.let { message ->
                        item(key = "chat_error", contentType = "contentType1") {
                            Surface(
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.17f),
                                shape = RoundedCornerShape(10.dp),
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.error.copy(alpha = 0.45f),
                                            RoundedCornerShape(10.dp)
                                        ),
                            ) {
                                Text(
                                    text = message,
                                    modifier = Modifier.padding(
                                        horizontal = 10.dp,
                                        vertical = 8.dp
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }

                    if (state.isBootstrapping && state.messages.isEmpty()) {
                        item(key = "chat_bootstrap", contentType = "contentType2") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }

                    items(
                        items = state.messages,
                        key = { it.localId },
                        contentType = { _ -> "contentType3" }) { message ->
                        ChatMessageBubble(
                            message = message,
                            onOpenReference = { href -> runCatching { uriHandler.openUri(href) } },
                        )
                    }

                    if (state.isBotTyping) {
                        item(key = "chat_typing", contentType = "contentType4") {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                BotBadge(size = 24.dp)
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(12.dp),
                                    tonalElevation = 1.dp,
                                ) {
                                    Text(
                                        text = "...",
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 8.dp
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                    }
                }

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (!hasUserMessages && !session?.starterTopics.isNullOrEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            session.starterTopics.forEach { topic ->
                                OutlinedButton(
                                    onClick = { viewModel.sendMessage(topic.label) },
                                    enabled = !state.isSending && !state.isBootstrapping,
                                    contentPadding = PaddingValues(
                                        horizontal = 10.dp,
                                        vertical = 0.dp
                                    ),
                                ) {
                                    Text(
                                        topic.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                        }
                    }

                    if (hasUserMessages && !latestBotMessage?.metadata?.quickReplies.isNullOrEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            latestBotMessage
                                .metadata
                                .quickReplies
                                .take(3)
                                .forEach { quickReply ->
                                    OutlinedButton(
                                        onClick = { viewModel.sendMessage(quickReply) },
                                        enabled = !state.isSending && !state.isBootstrapping,
                                        contentPadding = PaddingValues(
                                            horizontal = 10.dp,
                                            vertical = 0.dp
                                        ),
                                    ) {
                                        Text(
                                            quickReply,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1
                                        )
                                    }
                                }
                        }
                    }

                    OutlinedTextField(
                        value = state.inputText,
                        onValueChange = viewModel::onInputChanged,
                        placeholder = {
                            Text(if (isSpanish) "Escribe tu mensaje..." else "Write your message...")
                        },
                        enabled = !state.isSending && !state.isBootstrapping,
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        isError = charLimitReached,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(
                                onClick = {
                                    if (canUploadAttachment) {
                                        openAttachmentPicker = true
                                    }
                                },
                                enabled = canUploadAttachment && !state.isSending && !state.isBootstrapping,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            ) {
                                Text(if (isSpanish) "Adjuntar archivo" else "Attach file")
                            }

                            state.selectedAttachment?.let { attachment ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = attachment.fileName,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 170.dp),
                                    )
                                    TextButton(
                                        onClick = viewModel::clearAttachment,
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(
                                            text = if (isSpanish) "Quitar" else "Remove",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { viewModel.sendMessage() },
                            enabled =
                                !state.isSending &&
                                        !state.isBootstrapping &&
                                        !charLimitReached &&
                                        state.inputText.trim().isNotEmpty(),
                        ) {
                            Text(
                                if (state.isSending) {
                                    if (isSpanish) "Enviando..." else "Sending..."
                                } else {
                                    if (isSpanish) "Enviar" else "Send"
                                },
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text =
                                if (isSpanish) {
                                    "Adjuntos usados: ${state.attachmentCount}/$maxAttachments"
                                } else {
                                    "Attachments used: ${state.attachmentCount}/$maxAttachments"
                                },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "$charCount/$maxMessageChars",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (charLimitReached) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun ChatMessageBubble(
    message: ChatbotUiMessage,
    onOpenReference: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = message.senderRole == ChatbotSenderRole.User
    val isBot = message.senderRole == ChatbotSenderRole.Bot

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            if (isBot) {
                BotBadge(size = 24.dp)
            }

            Column(
                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Surface(
                    color =
                        when {
                            isUser -> MaterialTheme.colorScheme.primary
                            isBot -> MaterialTheme.colorScheme.surface
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                    contentColor =
                        when {
                            isUser -> MaterialTheme.colorScheme.onPrimary
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = if (isBot) sanitizeBotText(message.content) else message.content,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                message.metadata?.attachment?.let { attachment ->
                    Text(
                        text = attachment.originalFilename,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                val references = message.metadata?.references.orEmpty()
                if (isBot && references.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        references.take(5).forEach { reference ->
                            OutlinedButton(
                                onClick = { onOpenReference(reference.href) },
                                contentPadding = PaddingValues(
                                    horizontal = 8.dp,
                                    vertical = 0.dp
                                ),
                            ) {
                                Text(
                                    text = reference.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BotBadge(size: androidx.compose.ui.unit.Dp = 28.dp) {
    Box(
        modifier =
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center,
    ) {
        ChatbotBotIcon(
            modifier = Modifier.fillMaxSize(0.62f),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

private fun sanitizeBotText(raw: String): String {
    if (raw.isBlank()) return ""
    val withBreaks =
        raw
            .replace(Regex("(?i)<\\s*br\\s*/?>"), "\n")
            .replace(Regex("(?i)</\\s*p\\s*>"), "\n")
            .replace(Regex("(?i)</\\s*li\\s*>"), "\n")
    val noTags = withBreaks.replace(Regex("<[^>]+>"), "")
    return decodeBasicHtmlEntities(noTags).trim()
}

private fun decodeBasicHtmlEntities(raw: String): String =
    raw
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")

private fun detectAttachmentMimeType(
    selected: GalleryPhotoResult,
    bytes: ByteArray,
): String {
    val declared = selected.mimeType?.trim().orEmpty()
    if (declared.isNotBlank()) return declared

    if (bytes.size >= 4 &&
        bytes[0] == 0x25.toByte() &&
        bytes[1] == 0x50.toByte() &&
        bytes[2] == 0x44.toByte() &&
        bytes[3] == 0x46.toByte()
    ) {
        return "application/pdf"
    }

    return detectImageMimeType(bytes) ?: "application/octet-stream"
}

private fun fileExtensionForAttachmentMimeType(mimeType: String): String =
    when (mimeType.lowercase()) {
        "application/pdf" -> "pdf"
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/avif" -> "avif"
        else -> "jpg"
    }
