package com.billiardsdraw.vinceproshop

import android.app.Application

class VinceProShopApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidAppContext.initialize(this)
    }
}
