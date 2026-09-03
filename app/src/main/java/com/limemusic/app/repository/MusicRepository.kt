package com.limemusic.app.repository

import com.limemusic.app.data.MusicItem
import com.limemusic.app.parser.MusicNameParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRepository {

    private val githubRepository = GitHubMusicRepository()

    private var cachedMusic: List<MusicItem> = emptyList()
    private var initialized = false

    suspend fun getMusic(): Result<List<MusicItem>> =
        withContext(Dispatchers.IO) {
            if (initialized) {
                return@withContext Result.success(cachedMusic)
            }
            refresh()
        }

    suspend fun refresh(): Result<List<MusicItem>> =
        withContext(Dispatchers.IO) {
            val result = githubRepository.scanMusic()

            result.fold(
                onSuccess = { remoteList ->
                    val musicList = remoteList.mapNotNull { remote ->
                        runCatching {
                            parseRemoteMusic(remote)
                        }.getOrNull()
                    }

                    val sorted = musicList.sortedWith(
                        compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
                            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.artist }
                    )

                    cachedMusic = sorted
                    initialized = true

                    Result.success(sorted)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        }

    private fun parseRemoteMusic(
        remote: GitHubMusicRepository.RemoteMusic
    ): MusicItem {
        val parsed = MusicNameParser.parse(remote.fileName)
        val id = createMusicId(remote.path)

        return MusicItem(
            id = id,
            title = parsed.title,
            artist = parsed.artist,
            fileName = remote.fileName,
            path = remote.path,
            extension = parsed.extension,
            streamUrl = remote.cdnUrl,
            fallbackUrl = remote.rawUrl,
            githubUrl = remote.githubUrl
        )
    }

    private fun createMusicId(path: String): String =
        path.trim().lowercase().hashCode().toString()

    fun search(keyword: String): List<MusicItem> {
        val query = keyword.trim()
        if (query.isBlank()) return cachedMusic

        return cachedMusic.filter { music ->
            music.title.contains(query, ignoreCase = true) ||
                music.artist.contains(query, ignoreCase = true) ||
                music.fileName.contains(query, ignoreCase = true)
        }
    }

    fun findById(id: String): MusicItem? =
        cachedMusic.firstOrNull { it.id == id }

    fun findByPath(path: String): MusicItem? =
        cachedMusic.firstOrNull { it.path == path }

    fun size(): Int = cachedMusic.size

    fun isEmpty(): Boolean = cachedMusic.isEmpty()

    fun getCachedMusic(): List<MusicItem> = cachedMusic

    fun clearCache() {
        cachedMusic = emptyList()
        initialized = false
    }
}
