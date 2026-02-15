package com.billiardsdraw.vinceproshop.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import kotlinx.serialization.json.Json

expect fun createPlatformHttpClient(
    engine: HttpClientEngine,
    json: Json,
    enableLogs: Boolean = true,
): HttpClient

expect fun defaultApiBaseUrl(): String
