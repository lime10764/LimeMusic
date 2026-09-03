package com.limemusic.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.limemusic.app.data.MusicItem
import com.limemusic.app.player.MusicPlayerManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MusicPlayerViewModel(application: Application) :
    AndroidViewModel(application) {

    private val playerManager = MusicPlayerManager(application)

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentMusic = MutableStateFlow<MusicItem?>(null)
    val currentMusic: StateFlow<MusicItem?> = _currentMusic.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _queue =
        MutableStateFlow<List<MusicItem>>(emptyList())
    val queue: StateFlow<List<MusicItem>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _seeking = MutableStateFlow(false)
    val seeking: StateFlow<Boolean> = _seeking.asStateFlow()

    private var progressJob: Job? = null
    private var pendingAction: (() -> Unit)? = null

    private val playerListener = object : Player.Listener {

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (isPlaying) startProgressUpdates()
        }

        override fun onMediaItemTransition(
            mediaItem: MediaItem?,
            reason: Int
        ) {
            updateCurrentMusic()
            updateProgress()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            updateProgress()
            if (playbackState == Player.STATE_READY) {
                updateCurrentMusic()
            }
        }

        override fun onPlayerError(
            error: androidx.media3.common.PlaybackException
        ) {
            _error.value = error.message ?: "音乐播放失败"
            _isPlaying.value = false
            updateProgress()
        }
    }

    init {
        connectPlayer()
    }

    private fun connectPlayer() {
        playerManager.connect(
            onConnected = {
                _connected.value = true
                playerManager.addListener(playerListener)
                updateCurrentMusic()
                updateProgress()

                pendingAction?.let { action ->
                    pendingAction = null
                    action()
                }

                if (playerManager.isPlaying()) {
                    startProgressUpdates()
                }
            },
            onError = { throwable ->
                _connected.value = false
                _error.value = throwable.message ?: "播放器连接失败"
            }
        )
    }

    fun play(music: MusicItem) {
        _error.value = null
        _currentMusic.value = music

        val index = _queue.value.indexOfFirst { it.id == music.id }
        if (index >= 0) _currentIndex.value = index

        if (!playerManager.isConnected()) {
            pendingAction = {
                playerManager.play(music)
                startProgressUpdates()
            }
            return
        }

        playerManager.play(music)
        startProgressUpdates()
    }

    fun playQueue(
        musicList: List<MusicItem>,
        index: Int = 0
    ) {
        if (musicList.isEmpty()) return

        _error.value = null
        _queue.value = musicList

        val safeIndex = index.coerceIn(0, musicList.lastIndex)
        _currentIndex.value = safeIndex
        _currentMusic.value = musicList[safeIndex]

        if (!playerManager.isConnected()) {
            pendingAction = {
                playerManager.playQueue(musicList, safeIndex)
                startProgressUpdates()
            }
            return
        }

        playerManager.playQueue(musicList, safeIndex)
        startProgressUpdates()
    }

    fun setQueue(musicList: List<MusicItem>) {
        _queue.value = musicList
    }

    fun togglePlayPause() {
        if (!playerManager.isConnected()) return
        _error.value = null
        playerManager.togglePlayPause()
        startProgressUpdates()
    }

    fun pause() {
        playerManager.pause()
    }

    fun resume() {
        if (!playerManager.isConnected()) return
        playerManager.resume()
        startProgressUpdates()
    }

    fun next() {
        val list = _queue.value

        if (list.isEmpty()) {
            playerManager.next()
            updateCurrentMusic()
            return
        }

        val nextIndex =
            if (_currentIndex.value < 0 ||
                _currentIndex.value >= list.lastIndex
            ) 0 else _currentIndex.value + 1

        _currentIndex.value = nextIndex
        _currentMusic.value = list[nextIndex]

        playerManager.next()
        startProgressUpdates()
    }

    fun previous() {
        val list = _queue.value

        if (list.isEmpty()) {
            playerManager.previous()
            updateCurrentMusic()
            return
        }

        val previousIndex =
            if (_currentIndex.value <= 0) list.lastIndex
            else _currentIndex.value - 1

        _currentIndex.value = previousIndex
        _currentMusic.value = list[previousIndex]

        playerManager.previous()
        startProgressUpdates()
    }

    fun seekTo(positionMs: Long) {
        val duration = _durationMs.value

        val safePosition =
            if (duration > 0L) {
                positionMs.coerceIn(0L, duration)
            } else {
                positionMs.coerceAtLeast(0L)
            }

        _positionMs.value = safePosition
        updateProgress()
        playerManager.seekTo(safePosition)
    }

    fun beginSeek() {
        _seeking.value = true
    }

    fun seekFraction(fraction: Float) {
        val safeFraction = fraction.coerceIn(0f, 1f)
        val duration = _durationMs.value
        if (duration <= 0L) return

        val position = (duration * safeFraction).toLong()
        _positionMs.value = position
        _progress.value = safeFraction
    }

    fun endSeek() {
        val position = _positionMs.value
        _seeking.value = false
        seekTo(position)
    }

    fun clearError() {
        _error.value = null
    }

    private fun updateCurrentMusic() {
        val id = playerManager.getCurrentMediaId() ?: return

        val current = _queue.value.firstOrNull { it.id == id }
        if (current != null) {
            _currentMusic.value = current
            _currentIndex.value =
                _queue.value.indexOfFirst { it.id == id }
        }
    }

    private fun updateProgress() {
        if (_seeking.value) return

        val position = playerManager.getCurrentPosition()
        val duration = playerManager.getDuration()

        _positionMs.value = position
        _durationMs.value = duration

        _progress.value =
            if (duration > 0L) {
                (position.toFloat() / duration.toFloat())
                    .coerceIn(0f, 1f)
            } else {
                0f
            }
    }

    private fun startProgressUpdates() {
        if (progressJob?.isActive == true) return

        progressJob = viewModelScope.launch {
            while (true) {
                updateProgress()

                if (!playerManager.isPlaying()) {
                    break
                }

                delay(500L)
            }
        }
    }

    fun formattedPosition(): String =
        formatTime(_positionMs.value)

    fun formattedDuration(): String =
        formatTime(_durationMs.value)

    private fun formatTime(milliseconds: Long): String {
        if (milliseconds <= 0L) return "00:00"

        val totalSeconds = milliseconds / 1000L
        val seconds = totalSeconds % 60L
        val minutes = (totalSeconds / 60L) % 60L
        val hours = totalSeconds / 3600L

        return if (hours > 0L) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    fun hasCurrentMusic(): Boolean =
        _currentMusic.value != null

    fun hasQueue(): Boolean =
        _queue.value.isNotEmpty()

    fun queueSize(): Int =
        _queue.value.size

    fun clearPendingAction() {
        pendingAction = null
    }

    override fun onCleared() {
        progressJob?.cancel()
        pendingAction = null
        playerManager.removeListener(playerListener)
        playerManager.release()
        super.onCleared()
    }
}
