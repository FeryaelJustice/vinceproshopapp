package com.billiardsdraw.vinceproshop.data.security

interface AuthTokenStore {
    suspend fun saveToken(token: String)

    suspend fun readToken(): String?

    suspend fun clearToken()
}

class InMemoryAuthTokenStore : AuthTokenStore {
    private var token: String? = null

    override suspend fun saveToken(token: String) {
        this.token = token.trim().takeIf { it.isNotBlank() }
    }

    override suspend fun readToken(): String? = token

    override suspend fun clearToken() {
        token = null
    }
}
