package com.billiardsdraw.androidApp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.billiardsdraw.vinceproshop.App
import com.billiardsdraw.vinceproshop.AndroidActivityProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App()
        }
    }

    override fun onResume() {
        super.onResume()
        AndroidActivityProvider.update(this)
    }

    override fun onPause() {
        AndroidActivityProvider.update(null)
        super.onPause()
    }
}
