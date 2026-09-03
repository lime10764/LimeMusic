package com.limemusic.app.player

import android.app.PendingIntent
import android.content.Intent
import android.os.Build

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

import com.limemusic.app.MainActivity
import com.limemusic.app.R

@UnstableApi
class MusicPlaybackService : MediaSessionService() {

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_CHANNEL_ID = "lime_music_playback"
    }

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession

    override fun onCreate() {
        super.onCreate()

        setupNotification()
        initializePlayer()
        initializeMediaSession()
    }

    private fun setupNotification() {

        val notificationProvider =
            DefaultMediaNotificationProvider.Builder(this)
                .setNotificationId(NOTIFICATION_ID)
                .setChannelId(NOTIFICATION_CHANNEL_ID)
                .setChannelName(R.string.music_notification_channel)
                .build()

        setMediaNotificationProvider(notificationProvider)
    }

    private fun initializePlayer() {

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player = ExoPlayer.Builder(this)
            .build()
            .apply {

                setHandleAudioBecomingNoisy(true)

                setAudioAttributes(
                    audioAttributes,
                    true
                )

                // 播放列表循环
                repeatMode = Player.REPEAT_MODE_ALL

                addListener(
                    object : Player.Listener {

                        override fun onPlayerError(
                            error: PlaybackException
                        ) {

                            // 当前歌曲出错时尝试下一首
                            if (mediaItemCount > 1) {
                                seekToNextMediaItem()
                                prepare()
                                play()
                            }
                        }
                    }
                )
            }
    }

    private fun initializeMediaSession() {

        val openAppIntent =
            Intent(
                this,
                MainActivity::class.java
            ).apply {

                flags =
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

        val pendingIntentFlags =
            PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_IMMUTABLE
                } else {
                    0
                }

        val sessionActivity =
            PendingIntent.getActivity(
                this,
                100,
                openAppIntent,
                pendingIntentFlags
            )

        mediaSession =
            MediaSession.Builder(
                this,
                player
            )
                .setSessionActivity(sessionActivity)
                .build()
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession {
        return mediaSession
    }

    override fun onTaskRemoved(
        rootIntent: Intent?
    ) {

        // 正在播放时不要停止服务
        if (!player.isPlaying) {
            stopSelf()
        }

        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {

        if (::mediaSession.isInitialized) {
            mediaSession.release()
        }

        if (::player.isInitialized) {
            player.release()
        }

        super.onDestroy()
    }
}
