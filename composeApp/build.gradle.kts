import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun secretProperty(key: String, defaultValue: String = ""): String = (
    providers.gradleProperty(key).orNull
        ?: System.getenv(key)
        ?: localProperties.getProperty(key)
        ?: defaultValue
    )
    .replace("\r", "")
    .replace("\n", "")
    .trim()
    .removeSurrounding("\"")
    .removeSurrounding("'")

fun kotlinEscaped(value: String): String {
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
}

val configuredApiBaseUrlRaw = secretProperty(
    key = "VINCE_API_BASE_URL",
    defaultValue = "https://vinceproshop.com/api",
).trimEnd('/')

val configuredApiBaseUrl = if (configuredApiBaseUrlRaw.endsWith("/api")) {
    configuredApiBaseUrlRaw
} else {
    "${configuredApiBaseUrlRaw.trimEnd('/')}/api"
}

val configuredStripePublishableKey = secretProperty(
    key = "VINCE_STRIPE_PUBLISHABLE_KEY",
    defaultValue = "",
)

val configuredStripeMerchantDisplayName = secretProperty(
    key = "VINCE_STRIPE_MERCHANT_DISPLAY_NAME",
    defaultValue = "Vince Pro Shop",
)

val configuredHttpLogsEnabled = secretProperty(
    key = "VINCE_HTTP_LOGS_ENABLED",
    defaultValue = "true",
).toBooleanStrictOrNull() ?: true

val generatedSecretsDir =
    layout.buildDirectory.dir("generated/source/localSecrets/commonMain/kotlin")

val apiBaseUrl = configuredApiBaseUrl
val stripeKey = configuredStripePublishableKey
val merchantName = configuredStripeMerchantDisplayName
val httpLogs = configuredHttpLogsEnabled

val generateLocalSecrets by tasks.registering {
    outputs.dir(generatedSecretsDir)

    doLast {
        val packageDir = generatedSecretsDir?.get()?.asFile?.resolve("com/billiardsdraw/vinceproshop/core")
        packageDir?.mkdirs()
        packageDir?.resolve("LocalSecrets.kt")?.writeText(
            """
            package com.billiardsdraw.vinceproshop.core
            
            object LocalSecrets {
                const val API_BASE_URL: String = "${kotlinEscaped(apiBaseUrl)}"
                const val STRIPE_PUBLISHABLE_KEY: String = "${kotlinEscaped(stripeKey)}"
                const val STRIPE_MERCHANT_DISPLAY_NAME: String = "${kotlinEscaped(merchantName)}"
                const val HTTP_LOGS_ENABLED: Boolean = $httpLogs
            }
            
            fun localApiBaseUrl(): String = LocalSecrets.API_BASE_URL
            fun localStripePublishableKey(): String = LocalSecrets.STRIPE_PUBLISHABLE_KEY
            fun localStripeMerchantDisplayName(): String = LocalSecrets.STRIPE_MERCHANT_DISPLAY_NAME
            fun localHttpLogsEnabled(): Boolean = LocalSecrets.HTTP_LOGS_ENABLED
            """.trimIndent()
        )
    }
}

afterEvaluate {
    tasks.matching { task ->
        (task.name.startsWith("ksp") && task.name.contains("Ios", ignoreCase = true))
                || (task.name.contains("compile", ignoreCase = true)
                && task.name.contains("kotlin", ignoreCase = true))
    }.configureEach {
        dependsOn(generateLocalSecrets)
    }
}

kotlin {
    androidLibrary {
        namespace = "com.billiardsdraw.vinceproshop"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }

        androidResources {
            enable = true
        }

        withHostTest {
            isIncludeAndroidResources = true
        }

        packaging {
            resources {
                excludes += "/META-INF/{AL2.0,LGPL2.1}"
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generatedSecretsDir)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)

            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            implementation(libs.imagepickerkmp)

            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)

            // DataStore library
            implementation(libs.androidx.datastore)
            // The Preferences DataStore library
            implementation(libs.androidx.datastore.preferences)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.stripe.android)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.androidx.credentials)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

tasks.matching { task ->
    task.name.contains("compile", ignoreCase = true) &&
            task.name.contains("Kotlin", ignoreCase = true)
}.configureEach {
    dependsOn(generateLocalSecrets)
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)

    add("kspCommonMainMetadata", libs.room.compiler)
    add("kspAndroid", libs.room.compiler)
    add("kspIosX64", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)

    //debugImplementation(libs.ui.tooling)
}
