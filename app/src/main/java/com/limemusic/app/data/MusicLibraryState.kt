package com.limemusic.app.data

sealed interface MusicLibraryState {
    data object Idle : MusicLibraryState
    data object Loading : MusicLibraryState

    data class Success(
        val music: List<MusicItem>,
        val refreshedAt: Long = System.currentTimeMillis()
    ) : MusicLibraryState {
        val count: Int get() = music.size
        val isEmpty: Boolean get() = music.isEmpty()
    }

    data class Error(
        val message: String,
        val previousMusic: List<MusicItem> = emptyList()
    ) : MusicLibraryState {
        val hasPreviousMusic: Boolean
            get() = previousMusic.isNotEmpty()
    }
}
