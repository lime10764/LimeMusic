package com.limemusic.app.data

data class MusicItem(
    val id: String,
    val title: String,
    val artist: String,
    val fileName: String,
    val path: String,
    val extension: String,
    val streamUrl: String,
    val fallbackUrl: String,
    val githubUrl: String,
    val isFavorite: Boolean = false,
    val playCount: Long = 0L,
    val lastPlayedAt: Long = 0L,
    val durationMs: Long = -1L,
    val artworkUrl: String? = null
) {
    fun displayArtist(): String =
        if (artist.isBlank()) "未知歌手" else artist

    fun displayTitle(): String =
        if (title.isBlank()) fileName else title

    fun hasDuration(): Boolean = durationMs >= 0L

    fun formattedDuration(): String {
        if (durationMs < 0L) return "--:--"
        val totalSeconds = durationMs / 1000L
        val seconds = totalSeconds % 60L
        val totalMinutes = totalSeconds / 60L
        val minutes = totalMinutes % 60L
        val hours = totalMinutes / 60L

        return if (hours > 0L) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    fun withDuration(duration: Long): MusicItem =
        copy(durationMs = duration)

    fun toggleFavorite(): MusicItem =
        copy(isFavorite = !isFavorite)

    fun markPlayed(timestamp: Long = System.currentTimeMillis()): MusicItem =
        copy(
            playCount = playCount + 1L,
            lastPlayedAt = timestamp
        )
}
