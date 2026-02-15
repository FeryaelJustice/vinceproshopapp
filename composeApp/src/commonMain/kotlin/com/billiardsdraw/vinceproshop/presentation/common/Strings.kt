package com.billiardsdraw.vinceproshop.presentation.common

import com.billiardsdraw.vinceproshop.core.isSpanishLanguage

fun tr(en: String, es: String): String = if (isSpanishLanguage()) es else en
