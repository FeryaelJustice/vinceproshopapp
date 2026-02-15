package com.billiardsdraw.vinceproshop.domain.usecase

import com.billiardsdraw.vinceproshop.domain.repository.AccountRepository

class LogoutUseCase(
    private val repository: AccountRepository,
) {
    suspend operator fun invoke() {
        repository.logout()
    }
}
