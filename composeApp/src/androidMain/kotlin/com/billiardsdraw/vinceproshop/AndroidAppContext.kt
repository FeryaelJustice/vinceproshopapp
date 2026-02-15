package com.billiardsdraw.vinceproshop

import android.content.Context

internal object AndroidAppContext {
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun requireContext(): Context {
        return checkNotNull(appContext) {
            "Android app context is not initialized"
        }
    }
}
