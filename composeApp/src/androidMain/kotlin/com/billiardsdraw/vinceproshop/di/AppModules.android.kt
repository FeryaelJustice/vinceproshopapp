package com.billiardsdraw.vinceproshop.di

import com.billiardsdraw.vinceproshop.AndroidAppContext
import com.billiardsdraw.vinceproshop.data.security.AuthTokenStore
import com.billiardsdraw.vinceproshop.data.security.EncryptedDataStoreTokenStore
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module
    get() = module {
        single { OkHttp.create() }
        single<AuthTokenStore> { EncryptedDataStoreTokenStore(AndroidAppContext.requireContext()) }
    }
