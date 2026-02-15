package com.billiardsdraw.vinceproshop.core

import android.content.Context
import com.billiardsdraw.vinceproshop.AndroidAppContext
import java.util.Locale
import androidx.core.content.edit

private const val LANGUAGE_PREFS = "vinceproshop.localization"
private const val LANGUAGE_OPTION_KEY = "language_option"

actual fun platformLanguageCode(): String = Locale.getDefault().language

actual fun readStoredLanguageOption(): String? {
    return runCatching {
        val context = AndroidAppContext.requireContext()
        val prefs = context.getSharedPreferences(LANGUAGE_PREFS, Context.MODE_PRIVATE)
        prefs.getString(LANGUAGE_OPTION_KEY, null)
    }.getOrNull()
}

actual fun writeStoredLanguageOption(option: String?) {
    runCatching {
        val context = AndroidAppContext.requireContext()
        val prefs = context.getSharedPreferences(LANGUAGE_PREFS, Context.MODE_PRIVATE)
        prefs.edit { putString(LANGUAGE_OPTION_KEY, option) }
    }
}
