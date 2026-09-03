package com.limemusic.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Notification
import android.os.Build

class LimeMusicApplication : Application() {

    companion object {
        const val MUSIC_CHANNEL_ID = "lime_music_playback"
        const val MUSIC_CHANNEL_NAME = "音乐播放"
    }

    override fun onCreate() {
        super.onCreate()
        createMusicNotificationChannel()
    }

    private fun createMusicNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            MUSIC_CHANNEL_ID,
            MUSIC_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Lime Music 音乐播放控制"
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }
}
