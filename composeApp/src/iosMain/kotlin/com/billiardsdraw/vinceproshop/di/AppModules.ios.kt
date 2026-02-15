package com.billiardsdraw.vinceproshop.di

import com.billiardsdraw.vinceproshop.data.security.AuthTokenStore
import com.billiardsdraw.vinceproshop.data.security.IosEncryptedDataStoreTokenStore
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module
    get() = module {
        single { Darwin.create() }
        single<AuthTokenStore> { IosEncryptedDataStoreTokenStore() }
    }
