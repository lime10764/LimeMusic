package com.limemusic.app.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.limemusic.app.data.MusicItem

class MusicPlayerManager(context: Context) {

    private val applicationContext = context.applicationContext

    private val sessionToken = SessionToken(
        applicationContext,
        ComponentName(
            applicationContext,
            MusicPlaybackService::class.java
        )
    )

    private val controllerFuture: ListenableFuture<MediaController> =
        MediaController.Builder(
            applicationContext,
            sessionToken
        ).buildAsync()

    private var controller: MediaController? = null
    private var released = false

    fun isConnected(): Boolean = controller != null

    fun connect(
        onConnected: (() -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null
    ) {
        if (released) {
            onError?.invoke(
                IllegalStateException("播放器已经释放")
            )
            return
        }

        if (controller != null) {
            onConnected?.invoke()
            return
        }

        controllerFuture.addListener(
            {
                if (released) return@addListener

                try {
                    controller = controllerFuture.get()
                    onConnected?.invoke()
                } catch (error: Throwable) {
                    onError?.invoke(error.cause ?: error)
                }
            },
            ContextCompat.getMainExecutor(applicationContext)
        )
    }

    fun play(music: MusicItem) {
        val player = controller ?: return
        val currentMediaId = player.currentMediaItem?.mediaId

        if (currentMediaId == music.id) {
            player.play()
            return
        }

        player.setMediaItem(createMediaItem(music))
        player.prepare()
        player.play()
    }

    fun playQueue(
        musicList: List<MusicItem>,
        index: Int = 0
    ) {
        val player = controller ?: return
        if (musicList.isEmpty()) return

        val safeIndex = index.coerceIn(0, musicList.lastIndex)
        val mediaItems = musicList.map(::createMediaItem)

        player.setMediaItems(
            mediaItems,
            safeIndex,
            0L
        )
        player.prepare()
        player.play()
    }

    private fun createMediaItem(music: MusicItem): MediaItem {
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(music.displayTitle())
            .setArtist(music.displayArtist())
            .setDisplayTitle(music.displayTitle())
            .setSubtitle(music.displayArtist())

        music.artworkUrl
            ?.takeIf { it.isNotBlank() }
            ?.let { artwork ->
                metadataBuilder.setArtworkUri(Uri.parse(artwork))
            }

        return MediaItem.Builder()
            .setMediaId(music.id)
            .setUri(music.streamUrl)
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    fun pause() {
        controller?.pause()
    }

    fun resume() {
        controller?.play()
    }

    fun togglePlayPause() {
        val player = controller ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun stop() {
        controller?.stop()
    }

    fun next() {
        controller?.let { player ->
            if (player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
                player.play()
            }
        }
    }

    fun previous() {
        controller?.let { player ->
            if (player.hasPreviousMediaItem()) {
                player.seekToPreviousMediaItem()
                player.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun getCurrentPosition(): Long =
        controller?.currentPosition?.coerceAtLeast(0L) ?: 0L

    fun getDuration(): Long =
        controller?.duration?.takeIf { it >= 0L } ?: 0L

    fun isPlaying(): Boolean =
        controller?.isPlaying ?: false

    fun getCurrentMediaId(): String? =
        controller?.currentMediaItem?.mediaId

    fun getCurrentIndex(): Int =
        controller?.currentMediaItemIndex ?: -1

    fun getCurrentTitle(): String =
        controller?.mediaMetadata?.title?.toString() ?: ""

    fun getCurrentArtist(): String =
        controller?.mediaMetadata?.artist?.toString() ?: ""

    fun addListener(listener: Player.Listener) {
        controller?.addListener(listener)
    }

    fun removeListener(listener: Player.Listener) {
        controller?.removeListener(listener)
    }

    fun getController(): MediaController? = controller

    fun release() {
        if (released) return

        released = true
        controller?.release()
        controller = null
        controllerFuture.cancel(true)
    }
}
