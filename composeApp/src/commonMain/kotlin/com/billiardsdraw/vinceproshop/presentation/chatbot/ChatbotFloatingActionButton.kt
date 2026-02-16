package com.billiardsdraw.vinceproshop.presentation.chatbot

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PlatformChatbotFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
)
