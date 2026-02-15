package com.billiardsdraw.vinceproshop.core

expect fun currentLanguageCode(): String

fun isSpanishLanguage(): Boolean = currentLanguageCode().startsWith("es", ignoreCase = true)
