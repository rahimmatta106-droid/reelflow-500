package com.reelflow.platform

import android.app.Application

class PlatformApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Local-first app: no cloud backend, no Firebase init here.
    }
}
