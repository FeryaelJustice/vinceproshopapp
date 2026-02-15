package com.billiardsdraw.vinceproshop.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val DATASTORE_NAME = "secure_auth_store"
private const val KEYSTORE_ALIAS = "vinceproshop.jwt.aes"
private const val AES_MODE = "AES/GCM/NoPadding"
private const val GCM_TAG_BITS = 128
private val Context.secureAuthDataStore by preferencesDataStore(name = DATASTORE_NAME)
private val TOKEN_PREF_KEY = stringPreferencesKey("encrypted_jwt")

class EncryptedDataStoreTokenStore(
    private val appContext: Context,
) : AuthTokenStore {

    override suspend fun saveToken(token: String) {
        val normalized = token.trim()
        if (normalized.isBlank()) {
            clearToken()
            return
        }
        val encrypted = encrypt(normalized)
        appContext.secureAuthDataStore.edit { preferences ->
            preferences[TOKEN_PREF_KEY] = encrypted
        }
    }

    override suspend fun readToken(): String? {
        val encrypted = appContext.secureAuthDataStore.data
            .catch {
                if (it is IOException) emit(emptyPreferences()) else throw it
            }
            .map { preferences -> preferences[TOKEN_PREF_KEY] }
            .first()
            ?: return null

        return runCatching { decrypt(encrypted) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    override suspend fun clearToken() {
        appContext.secureAuthDataStore.edit { preferences ->
            preferences.remove(TOKEN_PREF_KEY)
        }
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(AES_MODE)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val cipherB64 = Base64.encodeToString(cipherText, Base64.NO_WRAP)
        return "$ivB64:$cipherB64"
    }

    private fun decrypt(encryptedPayload: String): String {
        val pieces = encryptedPayload.split(":")
        require(pieces.size == 2) { "Invalid encrypted JWT format." }
        val iv = Base64.decode(pieces[0], Base64.NO_WRAP)
        val cipherText = Base64.decode(pieces[1], Base64.NO_WRAP)
        val cipher = Cipher.getInstance(AES_MODE)
        val gcmSpec = GCMParameterSpec(GCM_TAG_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), gcmSpec)
        val plainBytes = cipher.doFinal(cipherText)
        return String(plainBytes, StandardCharsets.UTF_8)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = keyStore.getKey(KEYSTORE_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        val keySpec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(keySpec)
        return keyGenerator.generateKey()
    }
}
