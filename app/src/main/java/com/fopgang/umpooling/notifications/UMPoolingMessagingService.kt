package com.fopgang.umpooling.notifications

import com.google.firebase.messaging.FirebaseMessagingService

class UMPoolingMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FcmTokenSync.onTokenRefreshed(token)
    }
}
