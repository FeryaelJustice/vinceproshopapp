package com.billiardsdraw.vinceproshop.di

import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module
    get() = module { single { Darwin.create() } }