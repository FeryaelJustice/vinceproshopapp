package com.billiardsdraw.vinceproshop.data.security

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.posix.memcpy

private const val KEYCHAIN_SERVICE = "com.billiardsdraw.vinceproshop.login"
private const val KEYCHAIN_ACCOUNT = "saved_login_credential_v1"

private class IosLoginCredentialStore : LoginCredentialStore {

    override suspend fun readCredential(): LoginCredential? = memScoped {
        val query = mapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to KEYCHAIN_SERVICE,
            kSecAttrAccount to KEYCHAIN_ACCOUNT,
            kSecReturnData to true,
            kSecMatchLimit to kSecMatchLimitOne,
        )
        val out = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query as CFDictionaryRef, out.ptr)
        if (status == errSecItemNotFound.toInt()) return@memScoped null
        if (status != errSecSuccess) return@memScoped null

        val bytes = (out.value as? NSData)?.toByteArray() ?: return@memScoped null
        val raw = bytes.decodeToString()
        val separatorIndex = raw.indexOf('\n')
        if (separatorIndex <= 0 || separatorIndex >= raw.lastIndex) return@memScoped null
        val identifier = raw.substring(0, separatorIndex).trim()
        val password = raw.substring(separatorIndex + 1)
        if (identifier.isBlank() || password.isBlank()) return@memScoped null
        LoginCredential(identifier = identifier, password = password)
    }

    override suspend fun saveCredential(identifier: String, password: String) {
        val normalizedIdentifier = identifier.trim()
        if (normalizedIdentifier.isBlank() || password.isBlank()) return

        val payload = "$normalizedIdentifier\n$password".toNSData()
        val addQuery = mapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to KEYCHAIN_SERVICE,
            kSecAttrAccount to KEYCHAIN_ACCOUNT,
            kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
            kSecValueData to payload,
        )

        val addStatus = SecItemAdd(addQuery as CFDictionaryRef, null)
        if (addStatus == errSecSuccess) return
        if (addStatus != platform.Security.errSecDuplicateItem) return

        val matchQuery = mapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to KEYCHAIN_SERVICE,
            kSecAttrAccount to KEYCHAIN_ACCOUNT,
        )
        val updateValues = mapOf<Any?, Any?>(
            kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
            kSecValueData to payload,
        )
        SecItemUpdate(matchQuery as CFDictionaryRef, updateValues as CFDictionaryRef)
    }

    override suspend fun clearCredential() {
        val query = mapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to KEYCHAIN_SERVICE,
            kSecAttrAccount to KEYCHAIN_ACCOUNT,
        )
        SecItemDelete(query as CFDictionaryRef)
    }
}

actual fun provideLoginCredentialStore(): LoginCredentialStore = IosLoginCredentialStore()

private fun String.toNSData(): NSData {
    val nsString = NSString.create(string = this)
    return nsString.dataUsingEncoding(NSUTF8StringEncoding)
        ?: NSData.create()
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val lengthInt = length.toInt()
    if (lengthInt <= 0) return ByteArray(0)
    val src = bytes?.reinterpret<ByteVar>() ?: return ByteArray(0)
    return ByteArray(lengthInt).also { dst ->
        dst.usePinned { pinned ->
            memcpy(pinned.addressOf(0), src, lengthInt.convert())
        }
    }
}
