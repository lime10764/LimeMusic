package com.limemusic.app.repository

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class GitHubMusicRepository {

    companion object {
        private const val OWNER = "lime10764"
        private const val REPOSITORY = "music"
        private const val BRANCH = "main"
        private const val API_URL =
            "https://api.github.com/repos/$OWNER/$REPOSITORY/contents"
        private const val JSDELIVR_BASE =
            "https://cdn.jsdelivr.net/gh/$OWNER/$REPOSITORY@$BRANCH/"
        private const val RAW_BASE =
            "https://raw.githubusercontent.com/$OWNER/$REPOSITORY/$BRANCH/"

        private val MUSIC_EXTENSIONS = setOf(
            "mp3", "flac", "wav", "ogg", "m4a", "aac",
            "opus", "wma", "aiff", "ape", "wv", "caf", "webm"
        )
    }

    private val gson = Gson()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private data class GitHubContent(
        val name: String? = null,
        val path: String? = null,
        val type: String? = null,
        val download_url: String? = null,
        val html_url: String? = null
    )

    data class RemoteMusic(
        val fileName: String,
        val path: String,
        val cdnUrl: String,
        val rawUrl: String,
        val githubUrl: String,
        val extension: String
    )

    suspend fun scanMusic(): Result<List<RemoteMusic>> =
        withContext(Dispatchers.IO) {
            try {
                val result = mutableListOf<RemoteMusic>()
                scanDirectory("", result)
                result.sortWith(
                    compareBy(String.CASE_INSENSITIVE_ORDER) { it.fileName }
                )
                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun scanDirectory(
        path: String,
        result: MutableList<RemoteMusic>
    ) {
        val url = if (path.isBlank()) {
            API_URL
        } else {
            "$API_URL/${encodePath(path)}"
        }

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("GitHub 请求失败：HTTP ${response.code}")
            }

            val body = response.body.string()
            if (body.isBlank()) return

            val contents = try {
                gson.fromJson(body, Array<GitHubContent>::class.java)
            } catch (e: JsonSyntaxException) {
                throw IOException("GitHub 返回的数据格式异常", e)
            }

            for (item in contents) {
                val itemType = item.type?.lowercase() ?: continue
                val itemPath = item.path ?: continue

                when (itemType) {
                    "dir" -> scanDirectory(itemPath, result)
                    "file" -> {
                        if (isMusicFile(item.name ?: itemPath)) {
                            result.add(createMusicItem(item))
                        }
                    }
                }
            }
        }
    }

    private fun createMusicItem(item: GitHubContent): RemoteMusic {
        val path = item.path ?: throw IOException("音乐文件缺少 path")
        val fileName = item.name ?: path.substringAfterLast('/')
        val encodedPath = encodePath(path)
        val extension = getExtension(fileName)

        return RemoteMusic(
            fileName = fileName,
            path = path,
            cdnUrl = JSDELIVR_BASE + encodedPath,
            rawUrl = RAW_BASE + encodedPath,
            githubUrl =
                "https://github.com/$OWNER/$REPOSITORY/blob/$BRANCH/$encodedPath",
            extension = extension
        )
    }

    private fun isMusicFile(fileName: String): Boolean =
        getExtension(fileName).isNotEmpty()

    private fun getExtension(fileName: String): String {
        val cleanName = fileName.substringBefore('?')
        val dotIndex = cleanName.lastIndexOf('.')
        if (dotIndex < 0 || dotIndex == cleanName.length - 1) return ""

        val extension = cleanName.substring(dotIndex + 1).lowercase()
        return if (extension in MUSIC_EXTENSIONS) extension else ""
    }

    private fun encodePath(path: String): String =
        path.split("/").joinToString("/") { segment ->
            URLEncoder.encode(segment, Charsets.UTF_8.name()).replace("+", "%20")
        }
}
