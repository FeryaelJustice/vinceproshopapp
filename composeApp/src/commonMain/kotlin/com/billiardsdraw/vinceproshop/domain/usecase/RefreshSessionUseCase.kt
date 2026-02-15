package com.billiardsdraw.vinceproshop.domain.usecase

import com.billiardsdraw.vinceproshop.domain.model.AuthSession
import com.billiardsdraw.vinceproshop.domain.repository.AccountRepository

class RefreshSessionUseCase(
    private val repository: AccountRepository,
) {
    suspend operator fun invoke(): AuthSession = repository.refreshSession()
}
