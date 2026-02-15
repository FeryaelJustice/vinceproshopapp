package com.billiardsdraw.vinceproshop.core

import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

private const val LANGUAGE_OPTION_KEY = "vinceproshop.localization.language_option"

actual fun platformLanguageCode(): String {
    return NSLocale.currentLocale.languageCode
}

actual fun readStoredLanguageOption(): String? {
    val defaults = NSUserDefaults.standardUserDefaults
    return defaults.stringForKey(LANGUAGE_OPTION_KEY)
}

actual fun writeStoredLanguageOption(option: String?) {
    val defaults = NSUserDefaults.standardUserDefaults
    if (option == null) {
        defaults.removeObjectForKey(LANGUAGE_OPTION_KEY)
    } else {
        defaults.setObject(option, forKey = LANGUAGE_OPTION_KEY)
    }
    defaults.synchronize()
}
