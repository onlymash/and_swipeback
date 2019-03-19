package com.xbeats.swipebacksample

import android.app.Application
import com.aitangba.swipeback.ActivityLifecycleHelper

class App : Application() {
    companion object {
        lateinit var app: App
    }
    override fun onCreate() {
        super.onCreate()
        app = this
        registerActivityLifecycleCallbacks(ActivityLifecycleHelper.build())
    }
}