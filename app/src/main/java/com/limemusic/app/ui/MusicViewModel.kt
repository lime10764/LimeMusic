package com.limemusic.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limemusic.app.data.MusicItem
import com.limemusic.app.data.MusicLibraryState
import com.limemusic.app.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MusicViewModel : ViewModel() {

    private val repository = MusicRepository()

    private val _libraryState =
        MutableStateFlow<MusicLibraryState>(MusicLibraryState.Idle)
    val libraryState: StateFlow<MusicLibraryState> =
        _libraryState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _displayMusic =
        MutableStateFlow<List<MusicItem>>(emptyList())
    val displayMusic: StateFlow<List<MusicItem>> =
        _displayMusic.asStateFlow()

    private val _currentMusic =
        MutableStateFlow<MusicItem?>(null)
    val currentMusic: StateFlow<MusicItem?> =
        _currentMusic.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> =
        _isRefreshing.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> =
        _currentIndex.asStateFlow()

    fun loadMusic() {
        if (_libraryState.value is MusicLibraryState.Success) return
        refreshMusic()
    }

    fun refreshMusic() {
        if (_isRefreshing.value) return

        viewModelScope.launch {
            _isRefreshing.value = true
            _libraryState.value = MusicLibraryState.Loading

            val result = repository.refresh()

            result.fold(
                onSuccess = { music ->
                    _displayMusic.value =
                        applySearch(music, _searchQuery.value)

                    _libraryState.value =
                        MusicLibraryState.Success(music = music)
                },
                onFailure = { error ->
                    val oldMusic = repository.getCachedMusic()

                    _libraryState.value =
                        MusicLibraryState.Error(
                            message = error.message ?: "音乐库加载失败",
                            previousMusic = oldMusic
                        )

                    _displayMusic.value =
                        applySearch(oldMusic, _searchQuery.value)
                }
            )

            _isRefreshing.value = false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _displayMusic.value =
            applySearch(repository.getCachedMusic(), query)
    }

    fun search(query: String) = setSearchQuery(query)

    fun clearSearch() = setSearchQuery("")

    private fun applySearch(
        music: List<MusicItem>,
        query: String
    ): List<MusicItem> {
        if (query.isBlank()) return music

        return music.filter { item ->
            item.title.contains(query, ignoreCase = true) ||
                item.artist.contains(query, ignoreCase = true) ||
                item.fileName.contains(query, ignoreCase = true)
        }
    }

    fun selectMusic(music: MusicItem) {
        _currentMusic.value = music
        _currentIndex.value =
            _displayMusic.value.indexOfFirst { it.id == music.id }
    }

    fun selectMusicAt(index: Int) {
        val list = _displayMusic.value
        if (index < 0 || index >= list.size) return

        _currentIndex.value = index
        _currentMusic.value = list[index]
    }

    fun getNextMusic(): MusicItem? {
        val list = _displayMusic.value
        if (list.isEmpty()) return null

        val next = if (
            _currentIndex.value < 0 ||
            _currentIndex.value >= list.lastIndex
        ) 0 else _currentIndex.value + 1

        _currentIndex.value = next
        _currentMusic.value = list[next]
        return list[next]
    }

    fun getPreviousMusic(): MusicItem? {
        val list = _displayMusic.value
        if (list.isEmpty()) return null

        val previous =
            if (_currentIndex.value <= 0) list.lastIndex
            else _currentIndex.value - 1

        _currentIndex.value = previous
        _currentMusic.value = list[previous]
        return list[previous]
    }

    fun getPlaybackQueue(): List<MusicItem> =
        _displayMusic.value

    fun musicCount(): Int = repository.size()

    fun isMusicEmpty(): Boolean = repository.isEmpty()

    fun clearMusicCache() {
        repository.clearCache()
        _displayMusic.value = emptyList()
        _currentMusic.value = null
        _currentIndex.value = -1
        _libraryState.value = MusicLibraryState.Idle
    }

    fun reset() {
        _searchQuery.value = ""
        _currentMusic.value = null
        _currentIndex.value = -1
        _displayMusic.value = repository.getCachedMusic()
    }
}
