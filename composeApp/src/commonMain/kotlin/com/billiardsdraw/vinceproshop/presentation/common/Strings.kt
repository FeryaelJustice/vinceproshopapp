package com.billiardsdraw.vinceproshop.presentation.common

import com.billiardsdraw.vinceproshop.core.currentLanguageCode

fun tr(
    en: String,
    es: String,
): String {
    val languageCode = currentLanguageCode()
    return when {
        languageCode.startsWith("es", ignoreCase = true) -> es
        else -> en
    }
}
