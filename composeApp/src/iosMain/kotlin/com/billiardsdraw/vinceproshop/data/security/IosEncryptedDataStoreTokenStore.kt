package com.billiardsdraw.vinceproshop.data.security

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath
import platform.CommonCrypto.CCCrypt
import platform.CommonCrypto.kCCAlgorithmAES128
import platform.CommonCrypto.kCCBlockSizeAES128
import platform.CommonCrypto.kCCDecrypt
import platform.CommonCrypto.kCCEncrypt
import platform.CommonCrypto.kCCOptionPKCS7Padding
import platform.CommonCrypto.kCCSuccess
import platform.CoreCrypto.CCCrypt
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.Foundation.base64EncodedStringWithOptions
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
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
import platform.posix.arc4random_buf
import platform.posix.memcpy

private const val DATASTORE_DIR = "Library/Application Support/vinceproshop"
private const val DATASTORE_FILE = "secure_auth_store.preferences_pb"
private const val KEYCHAIN_SERVICE = "com.billiardsdraw.vinceproshop.jwt"
private const val KEYCHAIN_ACCOUNT = "jwt_aes_key_v1"
private const val KEY_SIZE_BYTES = 32
private const val IV_SIZE_BYTES = 16
private val TOKEN_PREF_KEY = stringPreferencesKey("encrypted_jwt")

class IosEncryptedDataStoreTokenStore : AuthTokenStore {

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        produceFile = { dataStoreFilePath().toPath() },
    )

    override suspend fun saveToken(token: String) {
        val normalized = token.trim()
        if (normalized.isBlank()) {
            clearToken()
            return
        }

        val encrypted = encrypt(normalized)
        dataStore.edit { prefs ->
            prefs[TOKEN_PREF_KEY] = encrypted
        }
    }

    override suspend fun readToken(): String? {
        val encrypted = dataStore.data
            .catch {
                emit(emptyPreferences())
            }
            .map { prefs -> prefs[TOKEN_PREF_KEY] }
            .first()
            ?: return null

        return runCatching { decrypt(encrypted) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    override suspend fun clearToken() {
        dataStore.edit { prefs ->
            prefs.remove(TOKEN_PREF_KEY)
        }
    }

    private fun encrypt(plainText: String): String {
        val key = getOrCreateAesKey()
        val iv = randomBytes(IV_SIZE_BYTES)
        val plain = plainText.encodeToByteArray()
        val cipher = aesCbcPkcs7(operation = kCCEncrypt, input = plain, key = key, iv = iv)
        return "${iv.toBase64()}:${cipher.toBase64()}"
    }

    private fun decrypt(payload: String): String {
        val parts = payload.split(":")
        require(parts.size == 2) { "Invalid encrypted payload format" }
        val iv = parts[0].fromBase64()
        val cipher = parts[1].fromBase64()
        val key = getOrCreateAesKey()
        val plain = aesCbcPkcs7(operation = kCCDecrypt, input = cipher, key = key, iv = iv)
        return plain.decodeToString()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun aesCbcPkcs7(
        operation: UInt,
        input: ByteArray,
        key: ByteArray,
        iv: ByteArray,
    ): ByteArray = memScoped {
        val output = ByteArray(input.size + kCCBlockSizeAES128.toInt())
        val outputLength = alloc<platform.posix.size_tVar>()

        val status = key.usePinned { keyPinned ->
            iv.usePinned { ivPinned ->
                input.usePinned { inputPinned ->
                    output.usePinned { outputPinned ->
                        CCCrypt(
                            op = operation,
                            alg = kCCAlgorithmAES128,
                            options = kCCOptionPKCS7Padding,
                            key = keyPinned.addressOf(0),
                            keyLength = key.size.convert(),
                            iv = ivPinned.addressOf(0),
                            dataIn = inputPinned.addressOf(0),
                            dataInLength = input.size.convert(),
                            dataOut = outputPinned.addressOf(0),
                            dataOutAvailable = output.size.convert(),
                            dataOutMoved = outputLength.ptr,
                        )
                    }
                }
            }
        }

        require(status == kCCSuccess) { "Crypto operation failed ($status)" }
        output.copyOf(outputLength.value.toInt())
    }

    private fun getOrCreateAesKey(): ByteArray {
        readKeyFromKeychain()?.let { return it }
        val generated = randomBytes(KEY_SIZE_BYTES)
        saveKeyToKeychain(generated)
        return generated
    }

    private fun readKeyFromKeychain(): ByteArray? = memScoped {
        val query = mapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to KEYCHAIN_SERVICE,
            kSecAttrAccount to KEYCHAIN_ACCOUNT,
            kSecReturnData to true,
            kSecMatchLimit to kSecMatchLimitOne,
        )

        val out = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query as CFDictionaryRef, out.ptr)
        if (status == errSecSuccess) {
            val data = out.value as NSData
            return@memScoped data.toByteArray()
        }
        if (status == errSecItemNotFound.toInt()) {
            return@memScoped null
        }
        error("Keychain read failed ($status)")
    }

    private fun saveKeyToKeychain(key: ByteArray) {
        val keyData = key.toNSData()
        val addQuery = mapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to KEYCHAIN_SERVICE,
            kSecAttrAccount to KEYCHAIN_ACCOUNT,
            kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
            kSecValueData to keyData,
        )

        val addStatus = SecItemAdd(addQuery as CFDictionaryRef, null)
        if (addStatus == errSecSuccess) return
        if (addStatus != platform.Security.errSecDuplicateItem) {
            error("Keychain add failed ($addStatus)")
        }

        val matchQuery = mapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to KEYCHAIN_SERVICE,
            kSecAttrAccount to KEYCHAIN_ACCOUNT,
        )
        val updateValues = mapOf<Any?, Any?>(
            kSecValueData to keyData,
            kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
        )
        val updateStatus = SecItemUpdate(
            matchQuery as CFDictionaryRef,
            updateValues as CFDictionaryRef,
        )
        require(updateStatus == errSecSuccess) { "Keychain update failed ($updateStatus)" }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun randomBytes(size: Int): ByteArray {
        val bytes = ByteArray(size)
        bytes.usePinned { pinned ->
            arc4random_buf(pinned.addressOf(0), size.convert())
        }
        return bytes
    }

    private fun dataStoreFilePath(): String {
        val base = NSHomeDirectory()
        val directory = "$base/$DATASTORE_DIR"
        NSFileManager.defaultManager.createDirectoryAtPath(
            path = directory,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        return "$directory/$DATASTORE_FILE"
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData =
    usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.convert())
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

private fun ByteArray.toBase64(): String = toNSData().base64EncodedStringWithOptions(0u)

private fun String.fromBase64(): ByteArray {
    val data = NSData.create(base64EncodedString = this, options = 0u)
        ?: error("Invalid Base64 payload")
    return data.toByteArray()
}
