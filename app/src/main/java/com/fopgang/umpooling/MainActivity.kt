
package com.fopgang.umpooling

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.fopgang.umpooling.auth.AuthViewModel
import com.fopgang.umpooling.ui.UMPoolingApp
import com.fopgang.umpooling.ui.theme.UMPoolingTheme

class MainActivity : ComponentActivity() {

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Token registration is independent of notification permission.
    }

    private fun requestNotificationPermission() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        com.fopgang.umpooling.notifications.NotificationLinks.accept(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            com.fopgang.umpooling.notifications.NotificationLinks.accept(intent)
        }

        requestNotificationPermission()
        enableEdgeToEdge()

        setContent {
            UMPoolingTheme {
                UMPoolingApp(
                    ViewModelProvider(this)[AuthViewModel::class.java]
                )
            }
        }
    }
}
