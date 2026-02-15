package com.billiardsdraw.vinceproshop

import android.app.Activity
import java.lang.ref.WeakReference

object AndroidActivityProvider {
    private var activityRef: WeakReference<Activity>? = null

    fun update(activity: Activity?) {
        activityRef = activity?.let(::WeakReference)
    }

    fun currentActivity(): Activity? = activityRef?.get()
}
