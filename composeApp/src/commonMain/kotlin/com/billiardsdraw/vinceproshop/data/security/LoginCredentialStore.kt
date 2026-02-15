package com.billiardsdraw.vinceproshop.data.security

data class LoginCredential(
    val identifier: String,
    val password: String,
)

interface LoginCredentialStore {
    suspend fun readCredential(): LoginCredential?

    suspend fun saveCredential(
        identifier: String,
        password: String,
    )

    suspend fun clearCredential()
}

expect fun provideLoginCredentialStore(): LoginCredentialStore
