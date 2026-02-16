package com.billiardsdraw.vinceproshop.presentation.chatbot

import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Suppress("ModifierDefaultValue")
@Composable
actual fun PlatformChatbotFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier,
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier.size(62.dp),
    ) {
        ChatbotBotIcon(
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onPrimary,
        )
    }
}
