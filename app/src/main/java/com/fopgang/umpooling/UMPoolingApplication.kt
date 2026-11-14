package com.fopgang.umpooling

import android.app.Application
import com.fopgang.umpooling.notifications.FcmTokenSync

class UMPoolingApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FcmTokenSync.start()
    }
}
