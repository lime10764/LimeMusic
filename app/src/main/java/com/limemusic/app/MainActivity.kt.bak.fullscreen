package com.limemusic.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.limemusic.app.ui.MainScreen
import com.limemusic.app.ui.MusicPlayerViewModel
import com.limemusic.app.ui.MusicViewModel

class MainActivity : ComponentActivity() {

    private val musicViewModel: MusicViewModel by viewModels()
    private val musicPlayerViewModel: MusicPlayerViewModel by viewModels()

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()

        setContent {
            MainScreen(
                musicViewModel = musicViewModel,
                playerViewModel = musicPlayerViewModel
            )
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        if (
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) return

        requestPermissions(
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            NOTIFICATION_PERMISSION_REQUEST
        )
    }
}
