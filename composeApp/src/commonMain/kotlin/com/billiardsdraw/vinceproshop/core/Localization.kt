package com.billiardsdraw.vinceproshop.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

expect fun platformLanguageCode(): String

expect fun readStoredLanguageOption(): String?

expect fun writeStoredLanguageOption(option: String?)

object LocalizationManager {
    private val _languageOption = MutableStateFlow(readStoredLanguageOption().orEmpty().ifBlank { "system" })
    private val _resolvedLanguageCode = MutableStateFlow(resolveLanguageCode(_languageOption.value))

    val languageOption: StateFlow<String> = _languageOption.asStateFlow()
    val resolvedLanguageCode: StateFlow<String> = _resolvedLanguageCode.asStateFlow()

    fun setLanguageOption(option: String) {
        val normalized = normalizeLanguageOption(option)
        _languageOption.value = normalized
        _resolvedLanguageCode.value = resolveLanguageCode(normalized)
        writeStoredLanguageOption(normalized)
    }

    private fun resolveLanguageCode(option: String): String =
        if (option.equals("system", ignoreCase = true)) {
            platformLanguageCode().ifBlank { "en" }
        } else {
            option.lowercase()
        }

    private fun normalizeLanguageOption(option: String): String = option.trim().lowercase().ifBlank { "system" }
}

@Composable
fun rememberCurrentLanguageCodeState(): State<String> = LocalizationManager.resolvedLanguageCode.collectAsStateWithLifecycle()

@Composable
fun rememberLanguageOptionState(): State<String> = LocalizationManager.languageOption.collectAsStateWithLifecycle()

fun currentLanguageCode(): String = LocalizationManager.resolvedLanguageCode.value

fun isSpanishLanguage(): Boolean = currentLanguageCode().startsWith("es", ignoreCase = true)

fun isRtlLanguageCode(languageCode: String): Boolean {
    val normalized = languageCode.lowercase()
    return normalized.startsWith("ar") ||
        normalized.startsWith("fa") ||
        normalized.startsWith("he") ||
        normalized.startsWith("ur")
}
