package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.data.notifications.FcmNotificationManager
import com.example.ui.MainContainer
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.FishGardenViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: FishGardenViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            FcmNotificationManager.logFcmEvent("FCM: POST_NOTIFICATIONS permission granted by user")
        } else {
            FcmNotificationManager.logFcmEvent("FCM: POST_NOTIFICATIONS permission declined")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize Firebase Cloud Messaging & Notification Channels safely
        runCatching {
            FcmNotificationManager.initialize(applicationContext)
        }.onFailure {
            android.util.Log.w("MainActivity", "FCM init fallback: ${it.message}")
        }

        // 2. Request Notification Permission on Android 13+ (API 33+)
        runCatching {
            checkNotificationPermission()
        }

        // 3. Handle navigation from notification intent
        runCatching {
            handleIntent(intent)
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContainer(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val targetTab = intent.getStringExtra("EXTRA_NAVIGATE_TAB")
        if (targetTab == "MY_ORDERS") {
            viewModel.currentTab.value = AppTab.MY_ORDERS
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}


