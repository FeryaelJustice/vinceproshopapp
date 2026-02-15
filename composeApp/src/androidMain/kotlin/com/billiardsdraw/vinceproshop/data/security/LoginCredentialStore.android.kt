package com.billiardsdraw.vinceproshop.data.security

import android.content.Context
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.PasswordCredential
import com.billiardsdraw.vinceproshop.AndroidActivityProvider
import com.billiardsdraw.vinceproshop.AndroidAppContext
import androidx.core.content.edit

private const val PREFS_NAME = "vince_login_credential_fallback"
private const val PREF_IDENTIFIER = "identifier"
private const val PREF_PASSWORD = "password"

private class AndroidLoginCredentialStore(
    context: Context,
) : LoginCredentialStore {
    private val credentialManager = CredentialManager.create(context)
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun readCredential(): LoginCredential? {
        val activity = AndroidActivityProvider.currentActivity()
        if (activity != null) {
            runCatching {
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(GetPasswordOption())
                    .build()
                val response = credentialManager.getCredential(activity, request)
                val credential = response.credential as? PasswordCredential
                if (credential != null) {
                    val result = LoginCredential(
                        identifier = credential.id,
                        password = credential.password,
                    )
                    saveFallback(result.identifier, result.password)
                    return result
                }
            }
        }
        return readFallback()
    }

    override suspend fun saveCredential(identifier: String, password: String) {
        val normalizedIdentifier = identifier.trim()
        if (normalizedIdentifier.isBlank() || password.isBlank()) return

        saveFallback(normalizedIdentifier, password)

        val activity = AndroidActivityProvider.currentActivity() ?: return
        runCatching {
            val request = CreatePasswordRequest(
                id = normalizedIdentifier,
                password = password,
            )
            credentialManager.createCredential(activity, request)
        }
    }

    override suspend fun clearCredential() {
        prefs.edit { remove(PREF_IDENTIFIER).remove(PREF_PASSWORD) }
    }

    private fun saveFallback(identifier: String, password: String) {
        prefs.edit {
            putString(PREF_IDENTIFIER, identifier)
                .putString(PREF_PASSWORD, password)
        }
    }

    private fun readFallback(): LoginCredential? {
        val identifier = prefs.getString(PREF_IDENTIFIER, null)?.trim().orEmpty()
        val password = prefs.getString(PREF_PASSWORD, null).orEmpty()
        if (identifier.isBlank() || password.isBlank()) return null
        return LoginCredential(identifier = identifier, password = password)
    }
}

actual fun provideLoginCredentialStore(): LoginCredentialStore {
    return AndroidLoginCredentialStore(AndroidAppContext.requireContext())
}
