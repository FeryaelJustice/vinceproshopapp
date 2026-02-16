package com.billiardsdraw.vinceproshop.data.security

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ptr
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRetain
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.kCFBooleanFalse
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal inline fun <T> withKeychainDictionary(
    vararg entries: Pair<CFStringRef?, Any?>,
    block: (CFDictionaryRef) -> T,
): T {
    val dictionary = createKeychainDictionary(entries)
    try {
        return block(dictionary)
    } finally {
        CFRelease(dictionary)
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun createKeychainDictionary(entries: Array<out Pair<CFStringRef?, Any?>>): CFDictionaryRef {
    val dictionary = CFDictionaryCreateMutable(
        allocator = null,
        capacity = 0,
        keyCallBacks = kCFTypeDictionaryKeyCallBacks.ptr,
        valueCallBacks = kCFTypeDictionaryValueCallBacks.ptr,
    ) ?: error("Unable to create CFDictionary for Keychain query")

    entries.forEach { (key, value) ->
        val nonNullKey = key ?: error("Keychain key cannot be null")
        when (value) {
            null -> Unit
            is Boolean -> {
                val boolValue = if (value) kCFBooleanTrue else kCFBooleanFalse
                CFDictionaryAddValue(dictionary, nonNullKey, boolValue)
            }
            is String -> addRetainedValue(dictionary, nonNullKey, CFBridgingRetain(value))
            is NSData -> addRetainedValue(dictionary, nonNullKey, CFBridgingRetain(value))
            else -> {
                val cfValue = value as? CFTypeRef
                    ?: error("Unsupported Keychain value type: ${value::class.simpleName}")
                CFDictionaryAddValue(dictionary, nonNullKey, cfValue)
            }
        }
    }

    return dictionary
}

@OptIn(ExperimentalForeignApi::class)
private fun addRetainedValue(dictionary: CFDictionaryRef, key: CFStringRef, value: CFTypeRef?) {
    CFDictionaryAddValue(dictionary, key, value)
    if (value != null) {
        CFRelease(value)
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun cfTypeRefToNSData(value: CFTypeRef?): NSData? {
    val retained = CFRetain(value) ?: return null
    return CFBridgingRelease(retained) as? NSData
}
