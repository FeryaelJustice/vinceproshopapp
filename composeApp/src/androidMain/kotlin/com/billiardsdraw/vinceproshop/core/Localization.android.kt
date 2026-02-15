package com.billiardsdraw.vinceproshop.core

import java.util.Locale

actual fun currentLanguageCode(): String = Locale.getDefault().language
