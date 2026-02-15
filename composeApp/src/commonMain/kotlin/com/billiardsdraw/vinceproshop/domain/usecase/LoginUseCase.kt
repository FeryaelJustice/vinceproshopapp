package com.billiardsdraw.vinceproshop.domain.usecase

import com.billiardsdraw.vinceproshop.domain.model.AuthSession
import com.billiardsdraw.vinceproshop.domain.repository.AccountRepository

class LoginUseCase(
    private val repository: AccountRepository,
) {
    suspend operator fun invoke(
        identifier: String,
        password: String,
    ): AuthSession = repository.login(identifier, password)
}
